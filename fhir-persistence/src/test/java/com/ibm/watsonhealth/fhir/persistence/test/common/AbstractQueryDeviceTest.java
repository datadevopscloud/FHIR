/**
 * (C) Copyright IBM Corp. 2016,2017,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.persistence.test.common;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.ibm.watsonhealth.fhir.model.Device;
import com.ibm.watsonhealth.fhir.model.Resource;
import com.ibm.watsonhealth.fhir.search.context.FHIRSearchContext;
import com.ibm.watsonhealth.fhir.search.util.SearchUtil;

/**
 *  This class contains a collection of tests that will be run against
 *  each of the various persistence layer implementations.
 *  There will be a subclass in each persistence project.
 */
public abstract class AbstractQueryDeviceTest extends AbstractPersistenceTest {
    
    /**
     * Tests the FHIRPersistenceCloudantImpl create API for a Device.
     * 
     * @throws Exception
     */
    @Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" })
    public void testCreateDevice() throws Exception {
        Device device = readResource(Device.class, "Device.json");

        persistence.create(getDefaultPersistenceContext(), device);
        assertNotNull(device);
        assertNotNull(device.getId());
        assertNotNull(device.getId().getValue());
        assertNotNull(device.getMeta());
        assertNotNull(device.getMeta().getVersionId().getValue());
        assertEquals("1", device.getMeta().getVersionId().getValue());
    }        
	
	/**
	 * Tests a query for a Device with manufacturer = 'Acme Devices, Inc' which should yield correct results
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDeviceQuery_manufacturer() throws Exception {
		List<Resource> resources = runQueryTest(Device.class, persistence, "manufacturer", "Acme Devices, Inc");
		assertNotNull(resources);
		assertTrue(resources.size() != 0);
		assertEquals(((Device)resources.get(0)).getManufacturer().getValue(),"Acme Devices, Inc");
	}
	
	/**
	 * Tests a query for a Device with model = 'AB45-J' which should yield no results
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDeviceQuery_model() throws Exception {
		List<Resource> resources = runQueryTest(Device.class, persistence, "model", "AB45-J");
		assertNotNull(resources);
		assertTrue(resources.size() == 0);
	}
	
	/**
	 * Tests a query for a Device with udi = '(01)00000123000017(10)ABC123(17)120415' which should yield correct results
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDeviceQuery_udi() throws Exception {
		List<Resource> resources = runQueryTest(Device.class, persistence, "udi", "(01)00000123000017(10)ABC123(17)120415");
		assertNotNull(resources);
		assertTrue(resources.size() != 0);
		assertEquals(((Device)resources.get(0)).getUdi().getValue(),"(01)00000123000017(10)ABC123(17)120415");
	}
	
	/*
	 * Pagination Testcases
	 */
	
	/**
	 * Tests a query with a resource type but without any query parameters. This should yield correct results using pagination
	 * 
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDevicePagination_001() throws Exception {
		
		Class<? extends Resource> resourceType = Device.class;
        Map<String, List<String>> queryParms = new HashMap<String, List<String>>();
		FHIRSearchContext context = SearchUtil.parseQueryParameters(resourceType, queryParms, null);
		context.setPageNumber(1);
		List<Resource> resources = persistence.search(getPersistenceContextForSearch(context), Device.class);
		assertNotNull(resources);
		assertTrue(resources.size() != 0);
		long count = context.getTotalCount();
		int pageSize = context.getPageSize();
		int lastPgNum = context.getLastPageNumber();
		assertEquals(context.getLastPageNumber(), (int) ((count + pageSize - 1) / pageSize));
		assertTrue((count > 10) ? (lastPgNum > 1) : (lastPgNum == 1));
	}
	
	/**
	 * Tests a query for a Device with udi = '(01)00000123000017(10)ABC123(17)120415' which should yield correct results using pagination
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDevicePagination_002() throws Exception {
		
		String parmName = "udi";
		String parmValue = "(01)00000123000017(10)ABC123(17)120415";
		Class<? extends Resource> resourceType = Device.class;
        Map<String, List<String>> queryParms = new HashMap<String, List<String>>();
		
		queryParms.put(parmName, Collections.singletonList(parmValue));
		FHIRSearchContext context = SearchUtil.parseQueryParameters(resourceType, queryParms, null);
		context.setPageNumber(1);
		List<Resource> resources = persistence.search(getPersistenceContextForSearch(context), Device.class);
		assertNotNull(resources);
		assertTrue(resources.size() != 0);
		assertEquals(((Device)resources.get(0)).getUdi().getValue(),"(01)00000123000017(10)ABC123(17)120415");
		long count = context.getTotalCount();
		int pageSize = context.getPageSize();
		int lastPgNum = context.getLastPageNumber();
		assertEquals(context.getLastPageNumber(), (int) ((count + pageSize - 1) / pageSize));
		assertTrue((count > 10) ? (lastPgNum > 1) : (lastPgNum == 1));
	}
	
	/**
	 * Tests a query for a Device with udi = '(01)00000123000017(10)(17)120415' which should yield no results using pagination
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDevicePagination_003() throws Exception {
		
		String parmName = "udi";
		String parmValue = "(01)00000123000017(10)(17)120415";
		Class<? extends Resource> resourceType = Device.class;
        Map<String, List<String>> queryParms = new HashMap<String, List<String>>();
		
		queryParms.put(parmName, Collections.singletonList(parmValue));
		FHIRSearchContext context = SearchUtil.parseQueryParameters(resourceType, queryParms, null);
		context.setPageNumber(1);
		List<Resource> resources = persistence.search(getPersistenceContextForSearch(context), Device.class);
		assertNotNull(resources);
		assertTrue(resources.size() == 0);
		long count = context.getTotalCount();
//		int lastPgNum = context.getLastPageNumber();
		assertTrue((count == 0)/* && (lastPgNum == Integer.MAX_VALUE)*/);
	}
	
	/**
	 * Tests a query for a Device with url = 'http://www.testdevice.ibm.com/bogusDeviceId/xxx' which should yield no results using pagination
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDevicePagination_urlNoResults() throws Exception {
		
		String parmName = "url";
		String parmValue = "http://www.testdevice.ibm.com/bogusDeviceId/xxx";
		Class<? extends Resource> resourceType = Device.class;
        Map<String, List<String>> queryParms = new HashMap<String, List<String>>();
		
		queryParms.put(parmName, Collections.singletonList(parmValue));
		FHIRSearchContext context = SearchUtil.parseQueryParameters(resourceType, queryParms, null);
		context.setPageNumber(1);
		List<Resource> resources = persistence.search(getPersistenceContextForSearch(context), Device.class);
		assertNotNull(resources);
		assertTrue(resources.size() == 0);
		long count = context.getTotalCount();
//		int lastPgNum = context.getLastPageNumber();
		assertTrue((count == 0)/* && (lastPgNum == Integer.MAX_VALUE)*/);
	}
	
	/**
	 * Tests a query for a Device with url = 'http://www.testdevice.ibm.com/bogusDeviceId' which should yield correct results using pagination
	 * @throws Exception
	 */
	@Test(groups = { "cloudant", "jpa", "jdbc", "jdbc-normalized" }, dependsOnMethods = { "testCreateDevice" })
	public void testDevicePagination_url() throws Exception {
		
		String parmName = "url";
		String parmValue = "http://www.testdevice.ibm.com/bogusDeviceId";
		Class<? extends Resource> resourceType = Device.class;
        Map<String, List<String>> queryParms = new HashMap<String, List<String>>();
		
		queryParms.put(parmName, Collections.singletonList(parmValue));
		FHIRSearchContext context = SearchUtil.parseQueryParameters(resourceType, queryParms, null);
		context.setPageNumber(1);
		List<Resource> resources = persistence.search(getPersistenceContextForSearch(context), Device.class);
		assertNotNull(resources);
		assertTrue(resources.size() != 0);
		assertEquals(((Device)resources.get(0)).getUrl().getValue(),"http://www.testdevice.ibm.com/bogusDeviceId");
		long count = context.getTotalCount();
		int pageSize = context.getPageSize();
		int lastPgNum = context.getLastPageNumber();
		assertEquals(context.getLastPageNumber(), (int) ((count + pageSize - 1) / pageSize));
		assertTrue((count > 10) ? (lastPgNum > 1) : (lastPgNum == 1));
	}
}
