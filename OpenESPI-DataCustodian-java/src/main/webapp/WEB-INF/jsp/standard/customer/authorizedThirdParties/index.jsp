<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 2013 EnergyOS.org
  ~
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<!DOCTYPE html>
<html lang="en">

<jsp:include page="../../tiles/head.jsp"/>

<body>

<jsp:include page="../../tiles/customer/header.jsp"/>

<div class="container">
    <div class="balance-section">
            <h2>Authorizations</h2>

            <table class="table table-striped" id="authorizations">
                <thead>
                <tr>
                    <th>DC</th>
		    <th>TP</th>
                    <th>Access Token</th>
                    <th>Status</th>
                    <th>Subscription ID</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="authorization" items="${authorizationList}">
                    <tr>
                        <td class="data_custodian"><c:out value="${authorization.applicationInformation.dataCustodianId}"/></td>
			<td name="third_party" class="data_custodian"><c:out value="${authorization.applicationInformation.thirdPartyApplicationName}"/></td>
                        <td class="access_token"><c:out value="${authorization.accessToken}"/></td>
                        <td class="status"><c:out value="${authorization.status}"/></td>
                        <td class="subscription_id"><c:out value="${authorization.resourceURI}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>

    <hr>

    <jsp:include page="../../tiles/footer.jsp"/>

</div>

</body>
</html>
