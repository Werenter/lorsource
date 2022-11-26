<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="reactions" required="true" type="java.util.Map" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<c:if test="${reactionsEnabled}">
  <c:if test="${not empty reactions}">
    <div class="reactions">
      <c:forEach var="r" items="${reactions}">
        <c:out value="${r.key}" escapeXml="true"/> ${r.value.count}:

        <c:forEach var="user" items="${r.value.topUsers}">
          ${user.nick}
        </c:forEach>
      </c:forEach>
    </div>
  </c:if>
</c:if>


