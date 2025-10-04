/**
 * PlatinumRemodelledProcessor - Processes Silver records to create Platinum records
 * 
 * KEY CHANGES FROM SILVER PROCESSOR:
 * 
 * 1. REMOVED COMPLEX PARTY MAPPING LOGIC:
 *    - No LEI resolution (getLeis, getPartyList methods)
 *    - No party validation (isAnyPartyListNull methods)
 *    - No complex party combinations (createSilverRecordsList methods)
 *    - Removed: getLEIsFromPOEAttributes, getLEIsFromCanadaRepAttributes, getLEIsFromPRAttributes
 *    - Removed: createCanadaBCRSilverRecord, createCanadaRepSilverRecord methods
 *    - Removed: createSilverRecordFromPOEParties, createSilverCanadaRepRecordsList methods
 * 
 * 2. REMOVED POE (PARTY-OPPARTY-ENTITY) LOGIC:
 *    - No opPartyId, entity fields in new Platinum model
 *    - No POE key generation (generatePoeWithClientID simplified to just prPartyId)
 *    - Removed: createCriteriaForPOE complex criteria (now just prPartyId + regProgram)
 *    - Removed: isInvalidSilverRecord validation (was checking opPartyId/entity/prPartyId)
 * 
 * 3. REMOVED CLIENT-LEVEL PROCESSING:
 *    - No clientId tracking or validation
 *    - No attributesTransformMap with clientId keys
 *    - No validClientIdSet/invalidClientIdSet logic
 *    - Removed: processAttributeMap method entirely
 *    - Removed: updateAttributes generic method
 * 
 * 4. REMOVED COMPLEX RECORD MERGING:
 *    - No changelog tracking (ChangeLogUtility usage removed)
 *    - No complex attribute merging (setCanadabcrLeis, setCanadarepLeis methods removed)
 *    - Simplified updateExistingRecord - just direct replacement
 * 
 * 5. REMOVED MANUAL RECORD PROCESSING:
 *    - No processManualRecords, upsertSilverRecord methods
 *    - No POEBronzeEvent handling
 *    - No manual record creation logic
 * 
 * 6. SIMPLIFIED DATA FLOW:
 *    - Input: Silver records (instead of Bronze)
 *    - Processing: Direct attribute extraction (instead of complex transformation)
 *    - Output: Simple Platinum records (instead of complex Silver with multiple clients)
 * 
 * 7. SURVIVOR LOGIC ADDED:
 *    - For UMRSECEXEMPTION: Takes survivor record from Silver's records array
 *    - New logic not present in Silver processor
 * 
 * WHAT REMAINS SIMILAR:
 * - Batch processing pattern
 * - Event publishing pattern  
 * - Repository save pattern
 * - Error handling and logging structure
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatinumRemodelledProcessor {

	private final PlatinumRemodelledRepository platinumRepository;
	private final DataProcessor dataProcessor;
	private final PartyCodaQueryClient partyCodaQueryClient;
	private final PlatinumRemodelledDao platinumDao;
	private final KafkaPublisher kafkaPublisher;
	private final MongoTemplate mongoTemplate;
	private final SilverRemodelledDao silverDao;
	private final VendorfeedDao vendorfeedDao;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public void processSilverRequest(SilverEvent silverEvent) {
    	if (isNotEmpty(silverEvent.getRegProgram())) {
            log.info("START: PlatinumProcessor.processSilverRequest() : Processing Silver Request for regProgram: {}",
                	silverEvent.getRegProgram());
        	processRequest(silverEvent.getRegProgram());
    	} else {
            log.error(
                	"PlatinumProcessor.processSilverRequest(): Invalid input, regProgram is not provided.");
    	}
	}

	public void processRequest(String regProgram) {
        log.info("START: PlatinumProcessor.processRequest(): Processing request for regProgram {}.", regProgram);
    	long methodStartTime = System.currentTimeMillis();
    	List<Document> silverBatchList = new ArrayList<>();
    	int batchNumber = 0;
    	long totalNumberOfRecordsProcessed = 0;
    	String queryRegProgram = RegProgramPlatinumSyncStatus.getPlatinumSyncStatus(regProgram);

    	MongoCollection<Document> collection = vendorfeedDao.getCollection(SILVER_COLLECTION);
    	Document query = new Document(queryRegProgram, BOOL_FALSE);

    	try (MongoCursor<Document> cursor = collection.find(query)
            	.batchSize(SILVER_BATCH_SIZE)
            	.iterator()) {
        	while (cursor.hasNext()) {
            	Document document = cursor.next();
            	silverBatchList.add(document);

            	if (silverBatchList.size() == SILVER_BATCH_SIZE) {
                	batchNumber++;
                	totalNumberOfRecordsProcessed += silverBatchList.size();
                	incrementBatch(silverBatchList, batchNumber, regProgram);
            	}
        	}
        	if (!silverBatchList.isEmpty()) {
            	batchNumber++;
            	totalNumberOfRecordsProcessed += silverBatchList.size();
            	incrementBatch(silverBatchList, batchNumber, regProgram);
        	}
    	} catch (Exception e) {
            log.error("PlatinumProcessor.processRequest(): Error while processing batch for regProgram {}: {}",
                	regProgram, e.getMessage(), e);
    	}
        log.info("PlatinumProcessor.processRequest(): Total batches {}, total records {} for regProgram {}.", batchNumber,
            	totalNumberOfRecordsProcessed, regProgram);

    	PlatinumEvent platinumEvent = new PlatinumEvent();
    	platinumEvent.setRegProgram(regProgram);
    	kafkaPublisher.publishPlatinumEvent(platinumEvent);
        log.info("END: PlatinumProcessor.processRequest() in {} ms.", System.currentTimeMillis() - methodStartTime);
	}

	private void incrementBatch(List<Document> silverBatchList, int batchNumber, String regProgram) {
    	try {
        	processBatch(silverBatchList, regProgram);
        	silverBatchList.clear();
            log.info("PlatinumProcessor batch : {} regprogram {}.", batchNumber, regProgram);
    	} catch (Exception e) {
            log.error("PlatinumProcessor.processRequest(): Error while processing batch for regProgram {}: {}",
                	regProgram, e.getMessage(), e);
    	}
	}

	private void processBatch(List<Document> silverDocumentBatchList, String regProgram) throws IOException {
    	Set<String> validPartyIdSet = new HashSet<>();
    	List<PlatinumRemodelled> platinumList = new ArrayList<>();

    	for (Document silverDocument : silverDocumentBatchList) {
        	SilverRemodelled vendorfeedSilverRecord;
        	try {
            	vendorfeedSilverRecord = OBJECT_MAPPER.convertValue(silverDocument, SilverRemodelled.class);
        	} catch (Exception e) {
                log.error("Failed to convert Document to Silver: {}", silverDocument, e);
            	continue;
        	}

        	String prPartyId = vendorfeedSilverRecord.getPrPartyId();
        	if (isEmpty(prPartyId)) {
            	continue;
        	}

        	try {
            	PlatinumRemodelled platinumRecord = createPlatinumRecord(vendorfeedSilverRecord, regProgram);
            	if (platinumRecord != null) {
                	platinumList.add(platinumRecord);
                	validPartyIdSet.add(prPartyId);
            	}
        	} catch (Exception e) {
                log.error("PlatinumProcessor.processBatch: Error while processing regProgram {} prPartyId {}", regProgram,
                    	prPartyId, e);
        	}
    	}

    	if (CollectionUtils.isNotEmpty(platinumList)) {
        	savePlatinumRecords(platinumList, regProgram);
    	}
    	if (CollectionUtils.isNotEmpty(validPartyIdSet)) {
        	silverDao.updateSilverRecord(validPartyIdSet, regProgram);
    	}
	}

	private PlatinumRemodelled createPlatinumRecord(SilverRemodelled silverRecord, String regProgram) {
    	PlatinumRemodelled platinumRecord = new PlatinumRemodelled();
    	platinumRecord.setPrPartyId(silverRecord.getPrPartyId());
    	platinumRecord.setRegProgram(regProgram);

    	switch (regProgram) {
        	case CANADABCR: {
            	if (silverRecord.getCanadabcr() != null) {
                	platinumRecord.setCanadabcr(createCanadaBCRPlatinum(silverRecord.getCanadabcr()));
            	}
            	break;
        	}
        	case CANADAREP: {
            	if (silverRecord.getCanadarep() != null) {
                	platinumRecord.setCanadarep(createCanadaRepPlatinum(silverRecord.getCanadarep()));
            	}
            	break;
        	}
        	case UMRSECEXEMPTION: {
            	if (silverRecord.getUmrsecexemption() != null) {
                	platinumRecord.setUmrsecexemption(createUMRSecExemptionPlatinum(silverRecord.getUmrsecexemption()));
            	}
            	break;
        	}
        	default: {
                log.warn("PlatinumProcessor.createPlatinumRecord: Unhandled regProgram: {}", regProgram);
            	return null;
        	}
    	}

    	return platinumRecord;
	}

	private CanadaBCRPlatinum createCanadaBCRPlatinum(CanadaBCRSilver canadaBCRSilver) {
    	CanadaBCRPlatinum canadaBCRPlatinum = new CanadaBCRPlatinum();
    	
    	if (canadaBCRSilver.getVendorRecord() != null && 
        	canadaBCRSilver.getVendorRecord().getAttributes() != null) {
        	canadaBCRPlatinum.setAttributes(canadaBCRSilver.getVendorRecord().getAttributes());
    	}
    	
    	return canadaBCRPlatinum;
	}

	private CanadaRepPlatinum createCanadaRepPlatinum(CanadaRepSilver canadaRepSilver) {
    	CanadaRepPlatinum canadaRepPlatinum = new CanadaRepPlatinum();
    	
    	if (canadaRepSilver.getVendorRecord() != null && 
        	canadaRepSilver.getVendorRecord().getAttributes() != null) {
        	canadaRepPlatinum.setAttributes(canadaRepSilver.getVendorRecord().getAttributes());
    	}
    	
    	return canadaRepPlatinum;
	}

	private UmrSecExemptionPlatinum createUMRSecExemptionPlatinum(UmrSecExemptionRemodelledSilver umrSecExemptionSilver) {
    	UmrSecExemptionPlatinum umrSecExemptionPlatinum = new UmrSecExemptionPlatinum();
    	
    	if (umrSecExemptionSilver.getUmrSecExemptionRecords() != null && 
        	!umrSecExemptionSilver.getUmrSecExemptionRecords().isEmpty()) {
        	
        	// For Platinum, we typically take the survivor record or the first record
        	UmrSecExemptionRemodelledRecord survivorRecord = umrSecExemptionSilver.getUmrSecExemptionRecords()
            	.stream()
            	.filter(record -> record.isSurvivor())
            	.findFirst()
            	.orElse(umrSecExemptionSilver.getUmrSecExemptionRecords().get(0));
            	
        	if (survivorRecord != null && survivorRecord.getAttributes() != null) {
            	umrSecExemptionPlatinum.setAttributes(survivorRecord.getAttributes());
        	}
    	}
    	
    	return umrSecExemptionPlatinum;
	}

	private void savePlatinumRecords(List<PlatinumRemodelled> newRecords, String regProgram) {
        log.info("START: PlatinumProcessor.savePlatinumRecords");
    	Date date = new Date();
    	List<PlatinumRemodelled> savePlatinumRecords = new ArrayList<>();
    	
    	// Get existing records for the same regProgram and prPartyIds
    	List<String> prPartyIds = newRecords.stream()
        	.map(PlatinumRemodelled::getPrPartyId)
        	.distinct()
        	.collect(Collectors.toList());
        	
    	Query query = new Query();
    	query.addCriteria(Criteria.where("regProgram").is(regProgram)
        	.and("prPartyId").in(prPartyIds));
        	
    	List<PlatinumRemodelled> existingRecords = platinumDao.findExistingRecords(query);
    	Map<String, PlatinumRemodelled> existingRecordMap = existingRecords.stream()
        	.collect(Collectors.toMap(PlatinumRemodelled::getPrPartyId, record -> record));

        log.info("PlatinumProcessor.savePlatinumRecords: Updating existing records {}", existingRecords.size());
        log.info("PlatinumProcessor.savePlatinumRecords: Creating new records {}", 
            newRecords.size() - existingRecords.size());

    	for (PlatinumRemodelled newRecord : newRecords) {
        	String prPartyId = newRecord.getPrPartyId();
        	PlatinumRemodelled existingRec = existingRecordMap.get(prPartyId);

        	if (existingRec != null) {
            	updateExistingRecord(existingRec, newRecord, regProgram, date);
            	savePlatinumRecords.add(existingRec);
        	} else {
            	newRecord.setVersionEffectiveTS(date);
            	savePlatinumRecords.add(newRecord);
        	}
    	}
    	
        log.info("PlatinumProcessor.savePlatinumRecords: {} Platinum Records getting saved for regProgram {}",
            	savePlatinumRecords.size(), regProgram);
    	platinumRepository.saveAll(savePlatinumRecords);
	}

	private void updateExistingRecord(PlatinumRemodelled existingRec, PlatinumRemodelled newRecord, 
                                  	String regProgram, Date date) {
    	switch (regProgram) {
        	case CANADABCR:
            	existingRec.setCanadabcr(newRecord.getCanadabcr());
            	break;
        	case CANADAREP:
            	existingRec.setCanadarep(newRecord.getCanadarep());
            	break;
        	case UMRSECEXEMPTION:
            	existingRec.setUmrsecexemption(newRecord.getUmrsecexemption());
            	break;
        	default:
                log.warn("PlatinumProcessor.updateExistingRecord: Unhandled regProgram: {}", regProgram);
            	break;
    	}
    	existingRec.setVersionEffectiveTS(date);
	}

	private void publishPlatinumEvent(String regProgram) {
    	PlatinumEvent platinumEvent = new PlatinumEvent();
    	platinumEvent.setRegProgram(regProgram);
    	kafkaPublisher.publishPlatinumEvent(platinumEvent);
	}
}