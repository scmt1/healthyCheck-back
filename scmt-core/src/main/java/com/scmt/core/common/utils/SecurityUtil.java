package com.scmt.core.common.utils;

import com.scmt.core.common.constant.CommonConstant;
import com.scmt.core.common.constant.SecurityConstant;
import com.scmt.core.common.exception.ScmtException;
import com.scmt.core.common.redis.RedisTemplateHelper;
import com.scmt.core.common.vo.TokenMember;
import com.scmt.core.common.vo.TokenUser;
import com.scmt.core.config.properties.ScmtAppTokenProperties;
import com.scmt.core.config.properties.ScmtTokenProperties;
import com.scmt.core.entity.Department;
import com.scmt.core.entity.Member;
import com.scmt.core.entity.Role;
import com.scmt.core.entity.User;
import com.scmt.core.service.DepartmentService;
import com.scmt.core.service.MemberService;
import com.scmt.core.service.UserService;
import com.scmt.core.service.mybatis.IUserRoleService;
import com.scmt.core.vo.PermissionDTO;
import com.scmt.core.vo.RoleDTO;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
@Component
public class SecurityUtil {

    @Autowired
    private ScmtTokenProperties tokenProperties;

    @Autowired
    private ScmtAppTokenProperties appTokenProperties;

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private IUserRoleService iUserRoleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private RedisTemplateHelper redisTemplate;

    public String getToken(String username, Boolean saveLogin) {

        if (StrUtil.isBlank(username)) {
            throw new ScmtException("username????????????");
        }
        Boolean saved = false;
        if (saveLogin == null || saveLogin) {
            saved = true;
            if (!tokenProperties.getRedis()) {
                tokenProperties.setTokenExpireTime(tokenProperties.getSaveLoginTime() * 60 * 24);
            }
        }
        // ??????token
        User u = userService.findByUsername(username);
        List<String> list = new ArrayList<>();
        // ????????????
        if (tokenProperties.getStorePerms()) {
            for (PermissionDTO p : u.getPermissions()) {
                if (StrUtil.isNotBlank(p.getTitle()) && StrUtil.isNotBlank(p.getPath())) {
                    list.add(p.getTitle());
                }
            }
            for (RoleDTO r : u.getRoles()) {
                list.add(r.getName());
            }
        }
        // ??????????????????token
        String token;
        if (tokenProperties.getRedis()) {
            // redis
            token = IdUtil.simpleUUID();
            TokenUser user = new TokenUser(u.getUsername(), list, saved);
            // ??????????????? ?????????token??????
            if (tokenProperties.getSdl()) {
                String oldToken = redisTemplate.get(SecurityConstant.USER_TOKEN + u.getUsername());
                if (StrUtil.isNotBlank(oldToken)) {
                    redisTemplate.delete(SecurityConstant.TOKEN_PRE + oldToken);
                }
            }
            if (saved) {
                redisTemplate.set(SecurityConstant.USER_TOKEN + u.getUsername(), token, tokenProperties.getSaveLoginTime(), TimeUnit.DAYS);
                redisTemplate.set(SecurityConstant.TOKEN_PRE + token, new Gson().toJson(user), tokenProperties.getSaveLoginTime(), TimeUnit.DAYS);
            } else {
                redisTemplate.set(SecurityConstant.USER_TOKEN + u.getUsername(), token, tokenProperties.getTokenExpireTime(), TimeUnit.MINUTES);
                redisTemplate.set(SecurityConstant.TOKEN_PRE + token, new Gson().toJson(user), tokenProperties.getTokenExpireTime(), TimeUnit.MINUTES);
            }
        } else {
            // JWT??????????????? ??????JWT????????????
            list = null;
            // JWT
            token = SecurityConstant.TOKEN_SPLIT + Jwts.builder()
                    //?????? ???????????????
                    .setSubject(u.getUsername())
                    //??????????????? ??????????????????????????????
                    .claim(SecurityConstant.AUTHORITIES, new Gson().toJson(list))
                    //????????????
                    .setExpiration(new Date(System.currentTimeMillis() + tokenProperties.getTokenExpireTime() * 60 * 1000))
                    //?????????????????????
                    .signWith(SignatureAlgorithm.HS512, SecurityConstant.JWT_SIGN_KEY)
                    .compact();
        }
        return token;
    }

    /**
     * ????????????????????????
     * @return
     */
    public User getCurrUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ScmtException("????????????????????????");
        }
        return userService.findByUsername(authentication.getName());
    }

    /**
     * ?????????????????????????????? null???????????????????????? ????????????-1??????????????????????????????
     */
    public List<String> getDeparmentIds() {

        List<String> deparmentIds = new ArrayList<>();
        User u = getCurrUser();
        // ????????????
        String key = "userRole::depIds:" + u.getId();
        String v = redisTemplate.get(key);
        if (StrUtil.isNotBlank(v)) {
            deparmentIds = new Gson().fromJson(v, new TypeToken<List<String>>() {
            }.getType());
            return deparmentIds;
        }
        // ????????????????????????
        List<Role> roles = iUserRoleService.findByUserId(u.getId());
        // ?????????????????????????????????
        Boolean flagAll = false;
        for (Role r : roles) {
            if (r.getDataType() == null || r.getDataType().equals(CommonConstant.DATA_TYPE_ALL)) {
                flagAll = true;
                break;
            }
        }
        // ????????????????????????null
        if (flagAll) {
            return null;
        }
        // ?????????????????? ?????????
        for (Role r : roles) {
            if (r.getDataType().equals(CommonConstant.DATA_TYPE_UNDER)) {
                // ??????????????????
                if (StrUtil.isBlank(u.getDepartmentId())) {
                    // ???????????????
                    deparmentIds.add("-1");
                } else {
                    // ???????????????????????????
                    List<String> ids = new ArrayList<>();
                    getRecursion(u.getDepartmentId(), ids);
                    deparmentIds.addAll(ids);
                }
            } else if (r.getDataType().equals(CommonConstant.DATA_TYPE_SAME)) {
                // ?????????
                if (StrUtil.isBlank(u.getDepartmentId())) {
                    // ???????????????
                    deparmentIds.add("-1");
                } else {
                    deparmentIds.add(u.getDepartmentId());
                }
            } else if (r.getDataType().equals(CommonConstant.DATA_TYPE_CUSTOM)) {
                // ?????????
                List<String> depIds = iUserRoleService.findDepIdsByUserId(u.getId());
                if (depIds == null || depIds.size() == 0) {
                    deparmentIds.add("-1");
                } else {
                    deparmentIds.addAll(depIds);
                }
            }
        }
        // ??????
        LinkedHashSet<String> set = new LinkedHashSet<>(deparmentIds.size());
        set.addAll(deparmentIds);
        deparmentIds.clear();
        deparmentIds.addAll(set);
        // ??????
        redisTemplate.set(key, new Gson().toJson(deparmentIds), 15L, TimeUnit.DAYS);
        return deparmentIds;
    }

    private void getRecursion(String departmentId, List<String> ids) {

        Department department = departmentService.get(departmentId);
        ids.add(department.getId());
        if (department.getIsParent() != null && department.getIsParent()) {
            // ???????????????
            List<Department> departments = departmentService.findByParentIdAndStatusOrderBySortOrder(departmentId, CommonConstant.STATUS_NORMAL);
            departments.forEach(d -> {
                getRecursion(d.getId(), ids);
            });
        }
    }

    /**
     * ???????????????????????????????????????
     * @param username
     */
    public List<GrantedAuthority> getCurrUserPerms(String username) {

        List<GrantedAuthority> authorities = new ArrayList<>();
        User user = userService.findByUsername(username);
        if (user == null || user.getPermissions() == null || user.getPermissions().isEmpty()) {
            return authorities;
        }
        for (PermissionDTO p : user.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(p.getTitle()));
        }
        return authorities;
    }

    public String getAppToken(String username, Integer platform) {

        if (StrUtil.isBlank(username)) {
            throw new ScmtException("username????????????");
        }
        // ??????token
        String token = IdUtil.simpleUUID();
        TokenMember member = new TokenMember(username, platform);
        String key = SecurityConstant.MEMBER_TOKEN + member.getUsername() + ":" + platform;
        // ??????????????? ?????????token??????
        if (appTokenProperties.getSpl()) {
            String oldToken = redisTemplate.get(key);
            if (StrUtil.isNotBlank(oldToken)) {
                redisTemplate.delete(SecurityConstant.TOKEN_MEMBER_PRE + oldToken);
            }
        }
        redisTemplate.set(key, token, appTokenProperties.getTokenExpireTime(), TimeUnit.DAYS);
        redisTemplate.set(SecurityConstant.TOKEN_MEMBER_PRE + token, new Gson().toJson(member), appTokenProperties.getTokenExpireTime(), TimeUnit.DAYS);
        return token;
    }

    /**
     * ????????????????????????
     * @return
     */
    public Member getCurrMember() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ScmtException("????????????????????????");
        }
        return memberService.findByUsername(authentication.getName());
    }

    /**
     * ????????????????????????????????????
     * @param username
     */
    public List<GrantedAuthority> getCurrMemberPerms(String username) {

        List<GrantedAuthority> authorities = new ArrayList<>();
        Member member = memberService.findByUsername(username);
        if (member == null || StrUtil.isBlank(member.getPermissions())) {
            return authorities;
        }
        String[] as = member.getPermissions().split(",");
        for (String a : as) {
            authorities.add(new SimpleGrantedAuthority(a));
        }
        return authorities;
    }
}
