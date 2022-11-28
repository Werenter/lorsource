<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="groups" type="java.util.List<ru.org.linux.group.Group>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${section.name}</title>
<link rel="parent" title="Linux.org.ru" href="/">
<link rel="alternate" href="/section-rss.jsp?section=${section.id}" type="application/rss+xml">
<meta name="format-detection" content="telephone=no">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${section.name}</h1>

<ul>

<c:forEach var="group" items="${groups}">
    <li>
      <a class="navLink" href="${group.url}">${group.title}</a>

      (${group.stat3} за сутки)

      <c:if test="${group.info != null}">
        — <em><c:out value="${group.info}" escapeXml="false"/></em>
      </c:if>

    </li>

  </c:forEach>

</ul>
<p>
  Все разделы форума также доступны в виде единой
  <a class="navLink" href="/forum/lenta/">ленты</a>.
</p>
<h2>RSS подписки</h2>

<ul>
  <li><a href="section-rss.jsp?section=${section.id}">Полный RSS форума</a></li>
  <li><a href="section-rss.jsp?section=${section.id}&filter=notalks">RSS без Talks</a></li>
  <li><a href="section-rss.jsp?section=${section.id}&filter=tech">RSS технических разделов форума</a></li>
</ul>

<p>Отдельные разделы форума также имеют RSS подписки, ссылки на них расположены на страницах
тем этих форумов.</p>

<c:if test="${not template.sessionAuthorized}">
<p>Если вы еще не зарегистрировались - вам <a href="/register.jsp">сюда</a>.</p>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
