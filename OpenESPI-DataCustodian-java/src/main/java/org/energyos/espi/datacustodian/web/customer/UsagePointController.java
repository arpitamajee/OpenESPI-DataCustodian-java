/*
 * Copyright 2013, 2014 EnergyOS.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.energyos.espi.datacustodian.web.customer;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.energyos.espi.common.domain.ApplicationInformation;
import org.energyos.espi.common.domain.ElectricPowerQualitySummary;
import org.energyos.espi.common.domain.ElectricPowerUsageSummary;
import org.energyos.espi.common.domain.MeterReading;
import org.energyos.espi.common.domain.Routes;
import org.energyos.espi.common.domain.TimeConfiguration;
import org.energyos.espi.common.domain.UsagePoint;
import org.energyos.espi.common.repositories.UsagePointDetailRepository;
import org.energyos.espi.common.service.ApplicationInformationService;
import org.energyos.espi.common.service.ExportService;
import org.energyos.espi.common.service.ResourceService;
import org.energyos.espi.common.service.UsagePointService;
import org.energyos.espi.common.utils.ExportFilter;
import org.energyos.espi.datacustodian.utils.UsagePointHelper;
import org.energyos.espi.datacustodian.web.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sun.syndication.io.FeedException;

@Controller
public class UsagePointController extends BaseController {

	@Autowired
	private UsagePointService usagePointService;

	@Autowired
	private ResourceService resourceService;

	@Autowired
	private ExportService exportService;

	@Autowired
	private ApplicationInformationService applicationInformationService;

	@Autowired
	private UsagePointDetailRepository usagePointDetailRepository;

	public void setUsagePointDetailRepository(
			UsagePointDetailRepository usagePointDetailRepository) {
		this.usagePointDetailRepository = usagePointDetailRepository;
	}

	@ModelAttribute
	public List<UsagePoint> usagePoints(Principal principal) {
		List<UsagePoint> usagePoints = usagePointService
				.findAllByRetailCustomer(currentCustomer(principal));
		populateExternalDetail(currentUser(principal).getCustomerId(),
				usagePoints);
		return usagePoints;
	}

	private List<UsagePoint> populateExternalDetail(String customerId,
			List<UsagePoint> usagePoints) {
		try {
			UsagePointHelper.populateExternalDetail(usagePoints,
					usagePointDetailRepository
							.findAllByRetailCustomerId(customerId));
		} catch (EmptyResultDataAccessException ignore) {

		}
		return usagePoints;
	}

	@RequestMapping(value = Routes.USAGE_POINT_INDEX, method = RequestMethod.GET)
	public String index() {
		return "/customer/usagepoints/index";
	}

	// DJ
	// @Transactional(readOnly = true, isolation = Isolation.DEFAULT,
	// propagation = Propagation.NEVER)
	@RequestMapping(value = Routes.USAGE_POINT_SHOW, method = RequestMethod.GET)
	public String show(@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, ModelMap model, Principal principal) {
		try {

			// because of the lazy loading from DB it's easier to build a bag
			// and hand it off
			// in a separate transaction, fill up a display bag lazily - do it
			// in a private method
			// so the transaction is scoped appropriately.

			HashMap<String, Object> displayBag = buildDisplayBag(
					currentUser(principal).getCustomerId(), retailCustomerId,
					usagePointId);

			model.put("displayBag", displayBag);
			model.put("usagePoint", displayBag.get("usagePoint"));

			return "/customer/usagepoints/show";
		} catch (Exception e) {
			e.printStackTrace();
			// go to do a dummy DB access to satify the transaction rollback
			// needs ...
			// TODO: may not be necessary
			resourceService.findById(1L, ApplicationInformation.class);
			System.out.printf("UX Error: %s\n", e.toString());
			model.put("errorString", e.toString());
			try {
				// try again (and maybe we can catch the rollback error ...
				return "/customer/error";
			} catch (Exception ex) {
				return "/customer/error";
			}
		}
	}

	@RequestMapping(value = Routes.USAGE_POINTS_FEED, method = RequestMethod.GET)
	public void feedEntries(HttpServletResponse response, Principal principal,
			@RequestParam Map<String, String> params) throws FeedException,
			IOException {
		response.setContentType(MediaType.APPLICATION_ATOM_XML_VALUE);
		response.addHeader("Content-Disposition",
				"attachment; filename=GreenButtonDownload.xml");
		exportService.exportUsagePointsFull(currentCustomer(principal).getId(),
				response.getOutputStream(), new ExportFilter(params));
	}

	@RequestMapping(value = Routes.USAGE_POINT_FEED, method = RequestMethod.GET)
	public void feedEntry(HttpServletResponse response, Principal principal,
			@PathVariable Long usagePointId,
			@RequestParam Map<String, String> params) throws FeedException,
			IOException {
		response.setContentType(MediaType.APPLICATION_ATOM_XML_VALUE);
		response.addHeader("Content-Disposition",
				"attachment; filename=GreenButtonDownload.xml");
		exportService.exportUsagePointFull(usagePointId,
				currentCustomer(principal).getId(), response.getOutputStream(),
				new ExportFilter(params));
	}

	@SuppressWarnings("rawtypes")
	// DJ
	// @Transactional(readOnly = true)
	public HashMap<String, Object> buildDisplayBag(String customerId,
			Long retailCustomerId, Long usagePointId) {

		HashMap<String, Object> displayBag = new HashMap<String, Object>();
		UsagePoint usagePoint = usagePointService.findById(usagePointId);

		ArrayList<UsagePoint> uplist = new ArrayList<UsagePoint>();
		uplist.add(usagePoint);
		populateExternalDetail(customerId, uplist);
		displayBag.put("usagePoint", usagePoint);
		displayBag.put("Description", usagePoint.getDescription());
		displayBag.put("ServiceCategory", usagePoint.getServiceCategory());
		displayBag.put("Uri", usagePoint.getSelfHref());
		displayBag.put("usagePointId", usagePoint.getId());
		// put the meterReadings
		List<HashMap> meterReadings = new ArrayList<HashMap>();
		Iterator<MeterReading> it = usagePoint.getMeterReadings().iterator();
		while (it.hasNext()) {
			HashMap<String, Object> mrBag = new HashMap<String, Object>();
			MeterReading mr = it.next();
			mrBag.put("Description", mr.getDescription());
			// TODO build the real IntervalBlocks URI
			String uriTail = "/RetailCustomer/" + retailCustomerId
					+ "/UsagePoint/" + usagePointId + "/MeterReading/"
					+ mr.getId() + "/show";
			mrBag.put(
					"Uri",
					applicationInformationService
							.getDataCustodianResourceEndpoint().replace(
									"/espi/1_1/resource", "")
							+ uriTail);
			mrBag.put("uriTail", uriTail);
			mrBag.put("ReadingType", mr.getReadingType().getDescription());
			meterReadings.add(mrBag);
		}
		displayBag.put("MeterReadings", meterReadings);
		// find the summary rollups
		List<ElectricPowerQualitySummary> qualitySummaryList = usagePoint
				.getElectricPowerQualitySummaries();
		List<ElectricPowerUsageSummary> usageSummaryList = usagePoint
				.getElectricPowerUsageSummaries();
		displayBag.put("QualitySummaryList", qualitySummaryList);
		displayBag.put("UsageSummaryList", usageSummaryList);

		TimeConfiguration timeConfiguration = usagePoint
				.getLocalTimeParameters();
		displayBag.put("localTimeParameters", timeConfiguration);
		return displayBag;
	}

	public void setUsagePointService(UsagePointService usagePointService) {
		this.usagePointService = usagePointService;
	}

	public void setExportService(ExportService exportService) {
		this.exportService = exportService;
	}
}