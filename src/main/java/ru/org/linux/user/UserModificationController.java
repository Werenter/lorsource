/*
 * Copyright 1998-2023 Linux.org.ru
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

package ru.org.linux.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.comment.CommentDeleteService;
import ru.org.linux.comment.DeleteCommentResult;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.Template;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class UserModificationController {
  private static final Logger logger = LoggerFactory.getLogger(UserModificationController.class);
  private final SearchQueueSender searchQueueSender;
  private final UserDao userDao;
  private final CommentDeleteService commentService;
  private final UserService userService;

  public UserModificationController(SearchQueueSender searchQueueSender, UserDao userDao,
                                    CommentDeleteService commentService, UserService userService) {
    this.searchQueueSender = searchQueueSender;
    this.userDao = userDao;
    this.commentService = commentService;
    this.userService = userService;
  }

  /**
   * Возвращает объект User модератора, если текущая сессия не модераторская, тогда исключение
   * @return текущий модератор
   */
  private static User getModerator() {
    Template tmpl = Template.getTemplate();
    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }
      return AuthUtil.getCurrentUser();
  }

  /**
   * Контроллер блокировки пользователя
   * @param user блокируемый пользователь
   * @param reason причина блокировки
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=block")
  public ModelAndView blockUser(
      @RequestParam("id") User user,
      @RequestParam(value = "reason", required = false) String reason
  ) {
    User moderator = getModerator();
    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
    }

    if (user.isBlocked()) {
      throw new UserErrorException("Пользователь уже блокирован");
    }

    userDao.block(user, moderator, reason);
    logger.info("User " + user.getNick() + " blocked by " + moderator.getNick());
    return redirectToProfile(user);
  }

  /**
   * Выставляем score=50 для пользователей у которых score меньше
   *
   * @param user кому ставим score
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=score50")
  public ModelAndView score50(
          @RequestParam("id") User user
  ) {
    User moderator = getModerator();
    if (user.isBlocked() || user.isAnonymous()) {
      throw new AccessViolationException("Нельзя выставить score=50 пользователю " + user.getNick());
    }

    userDao.score50(user, moderator);

    return redirectToProfile(user);
  }

  /**
   * Контроллер разблокировки пользователя
   * @param user разблокируемый пользователь
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=unblock")
  public ModelAndView unblockUser(
      @RequestParam("id") User user
  ) {

    User moderator = getModerator();
    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя разблокировать");
    }
    userDao.unblock(user, moderator);
    logger.info("User " + user.getNick() + " unblocked by " + moderator.getNick());
    return redirectToProfile(user);
  }

  private static ModelAndView redirectToProfile(User user) {
    return new ModelAndView(new RedirectView(getNoCacheLinkToProfile(user)));
  }

  private static String getNoCacheLinkToProfile(User user) {
    Random random = new Random();
    return "/people/" + URLEncoder.encode(user.getNick(), StandardCharsets.UTF_8) + "/profile?nocache=" + random.nextInt();
  }

  /**
   * Контроллер блокирования и полного удаления комментариев и топиков пользователя
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=block-n-delete-comments")
  public ModelAndView blockAndMassiveDeleteCommentUser(
      @RequestParam("id") User user,
      @RequestParam(value = "reason", required = false) String reason
  ) {
    User moderator = getModerator();
    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
    }

    if (user.isBlocked()) {
      throw new UserErrorException("Пользователь уже блокирован");
    }

    Map<String, Object> params = new HashMap<>();
    params.put("message", "Удалено");
    DeleteCommentResult deleteCommentResult = commentService.deleteAllCommentsAndBlock(user, moderator, reason);

    logger.info("User " + user.getNick() + " blocked by " + moderator.getNick());

    params.put("bigMessage",
            "Удалено комментариев: "+deleteCommentResult.getDeletedCommentIds().size()+"<br>"+
            "Удалено тем: "+deleteCommentResult.getDeletedTopicIds().size()
    );

    for (int topicId : deleteCommentResult.getDeletedTopicIds()) {
      searchQueueSender.updateMessage(topicId, true);
    }

    searchQueueSender.updateComment(deleteCommentResult.getDeletedCommentIds());

    return new ModelAndView("action-done", params);
  }

  /**
   * Контроллер смена признака корректора
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=toggle_corrector")
  public ModelAndView toggleUserCorrector(
      @RequestParam("id") User user
  ) {
    User moderator = getModerator();
    if (user.getScore()<UserService$.MODULE$.InviteScore()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя сделать корректором");
    }
    userDao.toggleCorrector(user, moderator);
    logger.info("Toggle corrector " + user.getNick() + " by " + moderator.getNick());

    return redirectToProfile(user);
  }

  /**
   * Сброс пароля пользователю
   * @param user пользователь которому сбрасываем пароль
   * @return сообщение о успешности сброса
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=reset-password")
  public ModelAndView resetPassword(@RequestParam("id") User user) {
    User moderator = getModerator();

    if (user.isModerator() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя сбросить пароль");
    }

    if (user.isAnonymous()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя сбросить пароль");
    }

    userDao.resetPassword(user, moderator);

    logger.info("Пароль "+user.getNick()+" сброшен модератором "+moderator.getNick());

    ModelAndView mv = new ModelAndView("action-done");
    mv.getModel().put("link", getNoCacheLinkToProfile(user));
    mv.getModel().put("message", "Пароль сброшен");

    return mv;
  }
  
  /**
   * Контроллер отчистки дополнительной информации в профиле
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=remove_userinfo")
  public ModelAndView removeUserInfo(@RequestParam("id") User user) {
    User moderator = getModerator();

    if (user.isModerator()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
    }

    userService.removeUserInfo(user, moderator);

    return redirectToProfile(user);
  }

  /**
   * Контроллер отчистки поля город
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=remove_town")
  public ModelAndView removeTown(@RequestParam("id") User user) {
    User moderator = getModerator();

    if (user.isModerator()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
    }

    userService.removeTown(user, moderator);

    return redirectToProfile(user);
  }

  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=remove_url")
  public ModelAndView removeUrl(@RequestParam("id") User user) {
    User moderator = getModerator();

    if (user.isModerator()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
    }

    userService.removeUrl(user, moderator);

    return redirectToProfile(user);
  }

  @RequestMapping(value="/remove-userpic.jsp", method= RequestMethod.POST)
  public ModelAndView removeUserpic(
    @RequestParam("id") User user
  ) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not autorized");
    }

      User currentUser = AuthUtil.getCurrentUser();

    // Не модератор не может удалять чужие аватары
    if (!tmpl.isModeratorSession() && currentUser.getId()!=user.getId()) {
      throw new AccessViolationException("Not permitted");
    }

    if (userDao.resetUserpic(user, currentUser)) {
      logger.info("Clearing " + user.getNick() + " userpic by " + currentUser.getNick());
    } else {
      logger.debug("SKIP Clearing " + user.getNick() + " userpic by " + currentUser.getNick());
    }

    return redirectToProfile(user);
  }

  /**
   * Контроллер заморозки и разморозки пользователя
   * @param user блокируемый пользователь
   * @param reason причина заморозки, общедоступна в дальнейшем
   * @param shift отсчёт времени от текущей точки, может быть отрицательным, в
   *              в результате даёт until, отрицательное значение используется
   *              для разморозки
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=freeze")
  public ModelAndView freezeUser(
      @RequestParam(name = "id") User user,
      @RequestParam(name = "reason") String reason,
      @RequestParam(name = "shift") String shift
  ) {

    if (reason.length() > 255) {
      throw new UserErrorException("Причина слишком длиная, максимум 255 байт");
    }

    User moderator = getModerator();
    Timestamp until = getUntil(shift);


    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заморозить");
    }

    if (user.isBlocked()) {
      throw new UserErrorException("Пользователь блокирован, его нельзя заморозить");
    }

    userDao.freezeUser(user, moderator, reason, until);
    logger.info("Freeze " + user.getNick() + " by " + moderator.getNick() + " until " + until);

    return redirectToProfile(user);
  }

  // get 'now', add the duration and returns result;
  // the duration can be negative
  private static Timestamp getUntil(String shift) {
    Duration  d   = Duration.parse(shift);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    now.setTime(now.getTime() + d.toMillis());
    return now;
  }

  @RequestMapping(value="/people/{nick}/profile/wipe", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView wipe(@PathVariable String nick) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("not moderator");
    }

    User user = userService.getUser(nick);

    if (!user.isBlockable()) {
      throw new AccessViolationException("Пользователя нельзя заблокировать");
    }

    if (user.isBlocked()) {
      throw new UserErrorException("Пользователь уже блокирован");
    }

    ModelAndView mv = new ModelAndView("wipe-user");
    mv.getModel().put("user", user);

    mv.getModel().put("commentCount", userDao.getExactCommentCount(user));

    return mv;
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(User.class, new UserIdPropertyEditor(userDao));
  }
}
