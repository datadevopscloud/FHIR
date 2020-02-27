/*
 * (C) Copyright IBM Corp. 2017,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test;

import static com.ibm.fhir.model.type.String.string;
import static com.ibm.fhir.model.type.Uri.uri;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.testng.annotations.Test;

import com.ibm.fhir.client.FHIRParameters;
import com.ibm.fhir.client.FHIRResponse;
import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.Boolean;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.Date;
import com.ibm.fhir.model.type.HumanName;
import com.ibm.fhir.model.type.Id;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.Reference;
import com.ibm.fhir.model.type.code.AdministrativeGender;
import com.ibm.fhir.model.type.code.BundleType;
import com.ibm.fhir.model.type.code.ObservationStatus;
import com.ibm.fhir.model.util.FHIRUtil;

/**
 * This class tests the REST API's compliance with the FHIR spec in terms of status code and OperationOutcome responses,
 * etc.
 */
public class ServerSpecTest extends FHIRServerTestBase {
    private static final JsonBuilderFactory BUILDER_FACTORY = Json.createBuilderFactory(null);
    private Patient savedPatient;
    @SuppressWarnings("unused")
    private Observation savedObservation;

    @Test(groups = { "server-spec" })
    public void testCreatePatient() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());
        URI location = response.getLocation();
        assertNotNull(location);
        assertNotNull(location.toString());
        assertFalse(location.toString().isEmpty());

        // Get the patient's logical id value.
        String patientId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new patient and verify it.
        response = target.path("Patient/" + patientId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        assertNotNull(responsePatient);
        savedPatient = responsePatient;
    }
    
    @Test(groups = { "server-spec" })
    public void testCreatePatientWithReturnPrefs() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request()
                                .header("Prefer", "return=minimal")
                                .post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());
        URI location = response.getLocation();
        assertNotNull(location);
        assertNotNull(location.toString());
        assertFalse(location.toString().isEmpty());
        assertEquals(response.getLength(),0);
        
        response = target.path("Patient").request()
                .header("Prefer", "return=representation")
                .post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        assertNotNull(responsePatient);
        
        response = target.path("Patient").request()
                .header("Prefer", "return=OperationOutcome")
                .post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());
        OperationOutcome oo = response.readEntity(OperationOutcome.class);
        assertNotNull(oo);
        assertTrue(oo.getIssue().toString().contains("dom-6: A resource should have narrative for robust management"));
    }

    // Test: create a new patient that contains an id
    /**
     * Test: create a new patient with a resource containing an id. This id
     * should be ignored and replaced by a different value.
     * @throws Exception
     */
    @Test(groups = { "server-spec" })
    public void testCreatePatientIgnoresId() throws Exception {
        WebTarget target = getWebTarget();
        Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");
        // Set an id on the patient.
        patient = patient.toBuilder().id("1").build();
        
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());
        URI location = response.getLocation();
        assertNotNull(location);
        assertNotNull(location.toString());
        assertFalse(location.toString().isEmpty());

        // Get the patient's logical id value.
        String newPatientId = getLocationLogicalId(response);
        
        // Check that this id DOES NOT match the id we included in the resource we submitted
        assertNotEquals(patient.getId(), newPatientId);
    }

    // Test: create an invalid patient
    @Test(groups = { "server-spec" })
    public void testCreatePatientErrorInvalidResource() throws Exception {
        WebTarget target = getWebTarget();
        
        JsonObject patient = BUILDER_FACTORY.createObjectBuilder().add("resourceType", "Patient")
                .build();
        
        Entity<JsonObject> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        
        assertValidationOperationOutcome(response.readEntity(OperationOutcome.class), "global-1");
    }

    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testUpdatePatient() throws Exception {
        WebTarget target = getWebTarget();

        // First, call the 'read' API to retrieve the previously-created patient.
        assertNotNull(savedPatient);
        Response response = target.path("Patient/" + savedPatient.getId()).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient patient = response.readEntity(Patient.class);
        assertNotNull(patient);
        
        // Modify the patient.
        patient = patient.toBuilder().gender(AdministrativeGender.MALE).build();
        
        // Next, update the patient and verify the response.
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        response = target.path("Patient/" + patient.getId()).request().put(entity, Response.class);
        assertResponse(response, Response.Status.OK.getStatusCode());
        URI location = response.getLocation();
        assertNotNull(location);
        assertNotNull(location.toString());
        assertFalse(location.toString().isEmpty());
    }

    @Test(groups = { "server-spec" }, dependsOnMethods={"testUpdatePatient"})
    public void testUpdatePatientVersionAware() throws Exception {
        WebTarget target = getWebTarget();

        // First, call the 'read' API to retrieve the previously-created patient.
        assertNotNull(savedPatient);
        Response response = target.path("Patient/" + savedPatient.getId()).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient patient = response.readEntity(Patient.class);
        assertNotNull(patient);
        
        // Modify the patient.
        patient = patient.toBuilder()
                .name(HumanName.builder().given(string("Jane")).family(string("Doe")).build())
                .gender(AdministrativeGender.FEMALE)
                .build();
        
        // Next, update the patient and verify the response.
        String ifMatchValue = "W/\"" + patient.getMeta().getVersionId().getValue() + "\"";
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        response = target.path("Patient/" + patient.getId())
                        .request()
                        .header("If-Match", ifMatchValue)
                        .put(entity, Response.class);
        assertResponse(response, Response.Status.OK.getStatusCode());
        URI location = response.getLocation();
        assertNotNull(location);
        assertNotNull(location.toString());
        assertFalse(location.toString().isEmpty());
    }

    @Test(groups = { "server-spec" }, dependsOnMethods={"testUpdatePatient"})
    public void testUpdatePatientVersionAwareError1() throws Exception {
        WebTarget target = getWebTarget();

        // First, call the 'read' API to retrieve the previously-created patient.
        assertNotNull(savedPatient);
        Response response = target.path("Patient/" + savedPatient.getId()).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient patient = response.readEntity(Patient.class);
        assertNotNull(patient);
        
        // Modify the patient.
        patient = patient.toBuilder()
                .name(HumanName.builder().given(string("Jane")).family(string("Doe")).build())
                .gender(AdministrativeGender.FEMALE)
                .build();
        
        // Next, update the patient and verify the response.
        // We'll use an incorrect value for the If-Match header (no W/" and " surrounding version id).
        String ifMatchValue = patient.getMeta().getVersionId().getValue();
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        response = target.path("Patient/" + patient.getId())
                        .request()
                        .header("If-Match", ifMatchValue)
                        .put(entity, Response.class);
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Invalid ETag value specified in request");
    }

    @Test(groups = { "server-spec" }, dependsOnMethods={"testUpdatePatient"})
    public void testUpdatePatientVersionAwareError2() throws Exception {
        WebTarget target = getWebTarget();

        // First, call the 'read' API to retrieve the previously-created patient.
        assertNotNull(savedPatient);
        Response response = target.path("Patient/" + savedPatient.getId()).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient patient = response.readEntity(Patient.class);
        assertNotNull(patient);
        
        // Modify the patient.
        patient = patient.toBuilder()
                .name(HumanName.builder().given(string("Jane")).family(string("Doe")).build())
                .gender(AdministrativeGender.FEMALE)
                .build();
        
        // Next, update the patient and verify the response.
        // We'll use an incorrect value for the If-Match header (no " around version id).
        String ifMatchValue = "W/" + patient.getMeta().getVersionId().getValue();
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        response = target.path("Patient/" + patient.getId())
                        .request()
                        .header("If-Match", ifMatchValue)
                        .put(entity, Response.class);
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Invalid ETag value specified in request");
    }

    @Test(groups = { "server-spec" }, dependsOnMethods={"testUpdatePatient"})
    public void testUpdatePatientVersionAwareError3() throws Exception {
        WebTarget target = getWebTarget();

        // First, call the 'read' API to retrieve the previously-created patient.
        assertNotNull(savedPatient);
        Response response = target.path("Patient/" + savedPatient.getId()).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient patient = response.readEntity(Patient.class);
        assertNotNull(patient);
        
        // Modify the patient.
        patient = patient.toBuilder()
                .name(HumanName.builder().given(string("Jane")).family(string("Doe")).build())
                .gender(AdministrativeGender.FEMALE)
                .build();
        
        // Next, update the patient and verify the response.
        // We'll use an incorrect value for the If-Match header (incorrect version #).
        String ifMatchValue = "W/\"1\"";
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        response = target.path("Patient/" + patient.getId())
                        .request()
                        .header("If-Match", ifMatchValue)
                        .put(entity, Response.class);
        assertResponse(response, Response.Status.PRECONDITION_FAILED.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "does not match current latest version of resource");
    }

    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testCreateObservation() throws Exception {
        WebTarget target = getWebTarget();
        
        // Next, create an Observation belonging to the new patient.
        String patientId = savedPatient.getId();
        Observation observation = TestUtil.buildPatientObservation(patientId, "Observation1.json");
        Entity<Observation> obs = Entity.entity(observation, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Observation").request().post(obs, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());
     
        String observationId = getLocationLogicalId(response);
        response = target.path("Observation/" + observationId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Observation responseObs = response.readEntity(Observation.class);
        savedObservation = responseObs;
    }    
    
   
    
    // Test: create an invalid observation
    @Test(groups = { "server-spec" })
    public void testCreateObservationErrorInvalidResource() throws Exception {
        WebTarget target = getWebTarget();
        
        
        JsonObject observation = BUILDER_FACTORY.createObjectBuilder().add("resourceType", "Observation")
                .build();
        
        Entity<JsonObject> entity = Entity.entity(observation, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Observation").request().post(entity, Response.class);
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertValidationOperationOutcome(response.readEntity(OperationOutcome.class), "Missing required element: 'status'");
    }

    // Test: include incorrect resource type in request body.
    @Test(groups = { "server-spec" })
    public void testCreatePatientErrorInvalidResourceType() throws Exception {
        WebTarget target = getWebTarget();

        // Build an Observation, then try to call the 'create patient' API.
        Observation observation = Observation.builder()
                .status(ObservationStatus.FINAL)
                .code(CodeableConcept.builder().coding(Coding.builder()
                        .system(uri("http://ibm.com/system"))
                        .code(Code.of("someCode")).build()).build())
                .build();

        Entity<Observation> entity = Entity.entity(observation, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource type 'Observation' does not match type specified in request URI: Patient");
    }

    // Test: include incorrect resource type in request body.
    @Test(groups = { "server-spec" })
    public void testCreateObservationErrorInvalidResourceType() {
        WebTarget target = getWebTarget();

        // Build an Observation, then try to call the 'create patient' API.
        Patient patient = buildPatient();
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Observation").request().post(entity, Response.class);

        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource type 'Patient' does not match type specified in request URI: Observation");
    }
    
    private Patient buildPatient() {
        String id = UUID.randomUUID().toString();
        
        Meta meta = Meta.builder()
                .versionId(Id.of("1"))
                .lastUpdated(Instant.now(ZoneOffset.UTC))
                .build();
        
        HumanName name = HumanName.builder()
                .given(string("John2"))
                .family(string("Doe2"))
                .build();
        
        return Patient.builder()
                .id(id)
                .meta(meta)
                .active(Boolean.TRUE)
                .name(name)
                .birthDate(Date.of("1980-01-01"))
                .build();
    }

    // Test: retrieve non-existent Patient.
    @Test(groups = { "server-spec" })
    public void testReadPatientErrorNotFound() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/123456789ABCDEF").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource 'Patient/123456789ABCDEF' not found.");
    }

    // Test: retrieve non-extent Observation.
    @Test(groups = { "server-spec" })
    public void testReadObservationErrorNotFound() {
        WebTarget target = getWebTarget();
        Response response = target.path("Observation/123456789ABCDEF").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource 'Observation/123456789ABCDEF' not found.");
    }

    // Test: retrieve non-existent MedicationDdministration
    @Test(groups = { "server-spec" })
    public void testReadMedicationAdministrationErrorNotFound() {
        WebTarget target = getWebTarget();

        // Try to retrieve a bogus MedicationAdministration.
        Response response = target.path("MedicationAdministration/123456789ABCDEF").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource 'MedicationAdministration/123456789ABCDEF' not found.");
    }

    // Test: retrieve invalid resource type.
    @Test(groups = { "server-spec" })
    public void testReadErrorInvalidResourceType() {
        WebTarget target = getWebTarget();
        Response response = target.path("BogusResourceType/1").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "'BogusResourceType' is not a valid resource type.");
    }

    // Test: retrieve non-existent Patient.
    @Test(groups = { "server-spec" })
    public void testVReadPatientErrorNotFound() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/123456789ABCDEF/_history/1").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource 'Patient/123456789ABCDEF' version 1 not found.");
    }

    // Test: retrieve non-existent Observation.
    @Test(groups = { "server-spec" })
    public void testVReadObservationErrorNotFound() {
        WebTarget target = getWebTarget();
        Response response = target.path("Observation/123456789ABCDEF/_history/1").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource 'Observation/123456789ABCDEF' version 1 not found.");
    }

    // Test: retrieve non-existent MedicationAdministration.
    @Test(groups = { "server-spec" })
    public void testVReadMedicationAdministrationErrorNotFound() {
        WebTarget target = getWebTarget();
        Response response = target.path("MedicationAdministration/123456789ABCDEF/_history/1").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Resource 'MedicationAdministration/123456789ABCDEF' version 1 not found.");
    }

    // Test: retrieve invalid resource type.
    @Test(groups = { "server-spec" })
    public void testVReadInvalidResourceType() {
        WebTarget target = getWebTarget();
        Response response = target.path("BogusResourceType/1/_history/1").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "'BogusResourceType' is not a valid resource type.");
    }

    // Test: retrieve invalid version.
    @Test(groups = { "server-spec" }, dependsOnMethods = { "testCreatePatient" })
    public void testVReadInvalidVersion() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId() + "/_history/-1").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "version -1 not found");
    }
    
    @Test
    public void testHistoryPatientNoResults() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/123456789ABCDEF/_history").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);

        assertNotNull(bundle.getType());
        assertNotNull(bundle.getType().getValue());
        assertEquals(BundleType.HISTORY.getValue(), bundle.getType().getValue());

        assertNotNull(bundle.getEntry());
        assertEquals(0, bundle.getEntry().size());
        
        assertNotNull(bundle.getTotal());
        assertTrue(0 == bundle.getTotal().getValue());
    }
    
    @Test(groups = { "server-spec" })
    public void testHistoryInvalidResourceType() {
        WebTarget target = getWebTarget();
        Response response = target.path("Bogus/123456789ABCDEF/_history").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "'Bogus' is not a valid resource type.");
    }
    
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testSearchPatientByFamilyName() {
        WebTarget target = getWebTarget();
        String familyName = savedPatient.getName().get(0).getFamily().getValue();
        Response response = target.path("Patient").queryParam("family", familyName).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);
        
        assertNotNull(bundle.getType());
        assertNotNull(bundle.getType().getValue());
        assertEquals(BundleType.SEARCHSET.getValue(), bundle.getType().getValue());
        
        assertNotNull(bundle.getEntry());
        assertTrue(bundle.getEntry().size() >= 1);
        
        assertNotNull(bundle.getTotal());
        assertNotNull(bundle.getTotal().getValue());
//      assertEquals(new Double(bundle.getEntry().size()), new Double(bundle.getTotal().getValue().doubleValue()));
    }
    
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testSearchPatientInvalidSearchAttribute() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient").queryParam("notasearch:parameter", "foo").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), 
                "Search parameter 'notasearch' for resource type 'Patient' was not found.");
    }
    
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient", "testCreateObservation"})
    public void testSearchObservation() {
        WebTarget target = getWebTarget();
        String patientId = savedPatient.getId();
        Response response = target.path("Observation").queryParam("subject", "Patient/" + patientId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
    }    
    
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreateObservation"})
    public void testSearchObservationInvalidSearchParameter() {
        WebTarget target = getWebTarget();
        Response response = target.path("Observation").queryParam("notasearch:parameter", "foo").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), 
                "Search parameter 'notasearch' for resource type 'Observation' was not found.");
    }
    
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreateObservation"})
    public void testSearchInvalidResourceType() {
        WebTarget target = getWebTarget();
        Response response = target.path("NotAResourceType").queryParam("notasearchparameter", "foo").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "'NotAResourceType' is not a valid resource type.");
    }
    
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testSearchPatientInvalidSearchOperator() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient").queryParam("family:xxx", "foo").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), "Undefined Modifier: xxx");
    }

    @Test(groups = { "server-spec" }, dependsOnMethods = { "testCreateObservation" })
    public void testConditionalCreateObservation() throws Exception {
        String fakePatientRef = "Patient/" + UUID.randomUUID().toString();
        Observation obs = TestUtil.readLocalResource("Observation1.json");
        obs = obs.toBuilder()
                .subject(Reference.builder().reference(string(fakePatientRef)).build())
                .build();
        
        // First conditional create should find no matches, so we should get back a 201.
        FHIRParameters ifNoneExistQuery = new FHIRParameters().searchParam("subject", fakePatientRef);
        FHIRResponse response = client.conditionalCreate(obs, ifNoneExistQuery);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
        String locationURI = response.getLocation();
        assertNotNull(locationURI);
        
        // Second conditional create should find 1 match, so we should get back a 200.
        response = client.conditionalCreate(obs, ifNoneExistQuery);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
        String locationURI2 = response.getLocation();
        assertNotNull(locationURI2);
        assertEquals(locationURI, locationURI2);
        
        // A search that results in multiple matches should result in a 412 status code.
        FHIRParameters multipleMatches = new FHIRParameters().searchParam("status", "final");
        response = client.conditionalCreate(obs, multipleMatches);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.PRECONDITION_FAILED.getStatusCode());
        
        // Finally, an invalid search should result in a 400 status code.
        FHIRParameters badSearch = new FHIRParameters().searchParam("NOTASEARCH:PARAM", "foo");
        response = client.conditionalCreate(obs, badSearch);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.BAD_REQUEST.getStatusCode());
    }    

    @Test(groups = { "server-spec" }, dependsOnMethods = { "testConditionalCreateObservation" })
    public void testConditionalUpdateObservation() throws Exception {
        String fakePatientRef = "Patient/" + UUID.randomUUID().toString();
        String obsId = UUID.randomUUID().toString();
        Observation obs = TestUtil.readLocalResource("Observation1.json");
        obs = obs.toBuilder()
                .subject(Reference.builder().reference(string(fakePatientRef)).build())
                .id(obsId)
                .build();
        
        // First conditional update should find no matches, so we should get back a 201.
        FHIRParameters query = new FHIRParameters().searchParam("_id", obsId);
        FHIRResponse response = client.conditionalUpdate(obs, query);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
        String locationURI = response.getLocation();
        assertNotNull(locationURI);
        
        // Second conditional update should find 1 match, so we should get back a 200.
        response = client.conditionalUpdate(obs, query);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
        String locationURI2 = response.getLocation();
        assertNotNull(locationURI2);
        
        // The location URIs should differ in the version #'s.
        assertNotEquals(locationURI, locationURI2);
        
        // Next, verify that we have two versions of the Observation resource.
        response = client.history("Observation", obsId, null);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
        Bundle historyBundle = response.getResource(Bundle.class);
        assertNotNull(historyBundle);
        assertEquals(2, historyBundle.getTotal().getValue().intValue());
        
        // A search that results in multiple matches should result in a 412 status code.
        FHIRParameters multipleMatches = new FHIRParameters().searchParam("status", "final");
        response = client.conditionalUpdate(obs, multipleMatches);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.PRECONDITION_FAILED.getStatusCode());
        
        // Finally, an invalid search should result in a 400 status code.
        FHIRParameters badSearch = new FHIRParameters().searchParam("NOTASEARCH:PARAM", "foo");
        response = client.conditionalUpdate(obs, badSearch);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    @Test(groups = { "server-spec" })
    public void testConditionalUpdateObservation2() throws Exception {
        String fakePatientRef = "Patient/" + UUID.randomUUID().toString();
        String obsId = UUID.randomUUID().toString();
        Observation obs = TestUtil.readLocalResource("Observation1.json");
        obs = obs.toBuilder()
                .subject(Reference.builder().reference(string(fakePatientRef)).build())
                .build();
        
        // First conditional update should find no matches, so we should get back a 201 
        // with server assigned id.
        FHIRParameters query = new FHIRParameters().searchParam("_id", obsId);
        FHIRResponse response = client.conditionalUpdate(obs, query);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
        String locationURI = response.getLocation();
        assertNotNull(locationURI);
        
        String[] tokens = parseLocationURI(locationURI);
        String resourceId = tokens[1];
        
        // Second conditional update should find 1 match, but because there is a un-matching
        // resourceId in the input resource, so we should get back a 400 error.
        query = new FHIRParameters().searchParam("_id", resourceId);
        obs = obs.toBuilder().id(obsId).build();
        response = client.conditionalUpdate(obs, query);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.getResource(OperationOutcome.class),
                "Input resource 'id' attribute must match the id of the search result resource");
        
    }
    
    // Test: retrieve Patient with _summary=true.
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testReadPatientSummary() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId())
                .queryParam("_summary", "true")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        Coding subsettedTag =
                Coding.builder().system(uri("http://terminology.hl7.org/CodeSystem/v3-ObservationValue")).code(Code.of("SUBSETTED")).display(string("subsetted")).build();
        assertTrue(FHIRUtil.hasTag(responsePatient, subsettedTag));
    }
    
    // Test: retrieve Patient with _summary=text.
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testReadPatientSummary_Text() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId())
                .queryParam("_summary", "text")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        Coding subsettedTag =
                Coding.builder().system(uri("http://terminology.hl7.org/CodeSystem/v3-ObservationValue")).code(Code.of("SUBSETTED")).display(string("subsetted")).build();
        assertTrue(FHIRUtil.hasTag(responsePatient, subsettedTag));
    }
    
    
    // Test: retrieve Patient with _summary=false.
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testReadPatientSummary_False() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId())
                .queryParam("_summary", "false")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        Coding subsettedTag =
                Coding.builder().system(uri("http://terminology.hl7.org/CodeSystem/v3-ObservationValue")).code(Code.of("SUBSETTED")).display(string("subsetted")).build();
        assertTrue(!FHIRUtil.hasTag(responsePatient, subsettedTag));
    }
    
    
    // Test: retrieve Patient with _summary=data.
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testReadPatientSummary_Data() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId())
                .queryParam("_summary", "data")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        Coding subsettedTag =
                Coding.builder().system(uri("http://terminology.hl7.org/CodeSystem/v3-ObservationValue")).code(Code.of("SUBSETTED")).display(string("subsetted")).build();
        assertTrue(FHIRUtil.hasTag(responsePatient, subsettedTag));
    }
    
    
    // Test: retrieve Patient with _summary=invalid.
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testReadPatientSummary_Invalid_lenient() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId())
                .queryParam("_summary", "invalid")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .header("Prefer", "handling=lenient")
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        Coding subsettedTag =
                Coding.builder().system(uri("http://terminology.hl7.org/CodeSystem/v3-ObservationValue")).code(Code.of("SUBSETTED")).display(string("subsetted")).build();
        assertTrue(!FHIRUtil.hasTag(responsePatient, subsettedTag));
    }
    
    
    // Test: retrieve Patient with _summary=invalid with "strict" Prefer header.
    @Test(groups = { "server-spec" }, dependsOnMethods={"testCreatePatient"})
    public void testReadPatientSummary_Invalid_strict() {
        WebTarget target = getWebTarget();
        Response response = target.path("Patient/" + savedPatient.getId())
                .queryParam("_summary", "invalid")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .header("Prefer", "handling=strict")
                .get();

        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class), 
                "An error occurred while parsing parameter '_summary'");
    }
 
}
