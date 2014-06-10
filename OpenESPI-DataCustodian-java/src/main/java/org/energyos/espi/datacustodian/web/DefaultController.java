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

package org.energyos.espi.datacustodian.web;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.energyos.espi.common.domain.Routes;
import org.energyos.espi.common.domain.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController extends BaseController {

	@RequestMapping(Routes.DEFAULT)
	public String defaultAfterLogin(HttpServletRequest request,
			Principal principal) {

		System.out.println(" princial --> " + request.getUserPrincipal());
		try {
			if (request.isUserInRole(User.ROLE_CUSTODIAN)) {
				return "redirect:/custodian/home";
			} else if (request.isUserInRole(User.ROLE_USER)) {
				if (currentCustomer(principal) == null) {
					return "/customer/nongbhome";
				}

				return "redirect:/RetailCustomer/"
						+ currentCustomer(principal).getId() + "/home";
			}
		} catch (Exception ignore) {
			return "redirect:/login";
		}
		return "redirect:/home";
	}
}