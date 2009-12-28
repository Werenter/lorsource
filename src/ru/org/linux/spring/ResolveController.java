/*
 * Copyright 1998-2009 Linux.org.ru
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

package ru.org.linux.spring;

import java.sql.Connection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Group;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Message;
import ru.org.linux.site.Template;
import ru.org.linux.site.User;

@Controller
public class ResolveController  {
  @RequestMapping("/resolve.jsp")
  public ModelAndView resolve(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam("msgid") int msgid,
    @RequestParam("resolve") String resolved
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      Message message = new Message(db, msgid);
      Group group = new Group(db, message.getGroupId());
      User currentUser = User.getCurrentUser(db, request.getSession());
      if (!group.isResolvable()) {
        throw new AccessViolationException("В данной группе нельзя помечать темы как решенные");
      }
      if (!tmpl.isModeratorSession() && currentUser.getId() != message.getUid()) {
        throw new AccessViolationException("У Вас нет прав на решение данной темы");
      }
      message.resolveMessage(db, (resolved != null) && "yes".equals(resolved));
      return new ModelAndView("action-done", "message", "Состояние темы изменено");
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }


}
