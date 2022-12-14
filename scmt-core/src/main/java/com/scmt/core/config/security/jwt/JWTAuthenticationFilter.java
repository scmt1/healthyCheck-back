package com.scmt.core.config.security.jwt;

import com.scmt.core.common.constant.SecurityConstant;
import com.scmt.core.common.redis.RedisTemplateHelper;
import com.scmt.core.common.utils.ResponseUtil;
import com.scmt.core.common.utils.SecurityUtil;
import com.scmt.core.common.vo.TokenMember;
import com.scmt.core.common.vo.TokenUser;
import com.scmt.core.config.properties.ScmtAppTokenProperties;
import com.scmt.core.config.properties.ScmtTokenProperties;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
@Slf4j
public class JWTAuthenticationFilter extends BasicAuthenticationFilter {

    private ScmtTokenProperties tokenProperties;

    private ScmtAppTokenProperties appTokenProperties;

    private RedisTemplateHelper redisTemplate;

    private SecurityUtil securityUtil;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,
                                   ScmtTokenProperties tokenProperties,
                                   ScmtAppTokenProperties appTokenProperties,
                                   RedisTemplateHelper redisTemplate, SecurityUtil securityUtil) {
        super(authenticationManager);
        this.tokenProperties = tokenProperties;
        this.appTokenProperties = appTokenProperties;
        this.redisTemplate = redisTemplate;
        this.securityUtil = securityUtil;
    }

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        String header = request.getHeader(SecurityConstant.HEADER);
        if (StrUtil.isBlank(header)) {
            header = request.getParameter(SecurityConstant.HEADER);
        }
        String appHeader = request.getHeader(SecurityConstant.APP_HEADER);
        if (StrUtil.isBlank(appHeader)) {
            appHeader = request.getParameter(SecurityConstant.APP_HEADER);
        }
        Boolean notValid = (StrUtil.isBlank(header) || (!tokenProperties.getRedis() && !header.startsWith(SecurityConstant.TOKEN_SPLIT)))
                && StrUtil.isBlank(appHeader);
        if (notValid) {
            chain.doFilter(request, response);
            return;
        }
        try {
            UsernamePasswordAuthenticationToken authentication = null;
            if (StrUtil.isNotBlank(header)) {
                authentication = getAuthentication(header, response);
            } else {
                authentication = getAppAuthentication(appHeader, response);
            }
            if (authentication == null) {
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn(e.toString());
        }

        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(String header, HttpServletResponse response) {

        // ?????????
        String username = null;
        // ??????
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (tokenProperties.getRedis()) {
            // redis
            String v = redisTemplate.get(SecurityConstant.TOKEN_PRE + header);
            if (StrUtil.isBlank(v)) {
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 401, "?????????????????????????????????"));
                return null;
            }
            TokenUser user = new Gson().fromJson(v, TokenUser.class);
            username = user.getUsername();
            if (tokenProperties.getStorePerms()) {
                // ???????????????
                for (String ga : user.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(ga));
                }
            } else {
                // ????????? ??????????????????
                authorities = securityUtil.getCurrUserPerms(username);
            }
            if (!user.getSaveLogin()) {
                // ????????????????????????????????????????????????
                redisTemplate.set(SecurityConstant.USER_TOKEN + username, header, tokenProperties.getTokenExpireTime(), TimeUnit.MINUTES);
                redisTemplate.set(SecurityConstant.TOKEN_PRE + header, v, tokenProperties.getTokenExpireTime(), TimeUnit.MINUTES);
            }
        } else {
            // JWT
            try {
                // ??????token
                Claims claims = Jwts.parser()
                        .setSigningKey(SecurityConstant.JWT_SIGN_KEY)
                        .parseClaimsJws(header.replace(SecurityConstant.TOKEN_SPLIT, ""))
                        .getBody();

                // ???????????????
                username = claims.getSubject();
                // JWT??????????????? ?????????????????? ??????JWT????????????
                authorities = securityUtil.getCurrUserPerms(username);
            } catch (ExpiredJwtException e) {
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 401, "?????????????????????????????????"));
            } catch (Exception e) {
                log.error(e.toString());
                e.printStackTrace();
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 500, "??????token??????"));
            }
        }

        if (StrUtil.isNotBlank(username)) {
            // ???????????? ??????password?????????null
            User principal = new User(username, "", authorities);
            return new UsernamePasswordAuthenticationToken(principal, null, authorities);
        }
        return null;
    }

    private UsernamePasswordAuthenticationToken getAppAuthentication(String appHeader, HttpServletResponse response) {

        // ?????????
        String username = null;

        String v = redisTemplate.get(SecurityConstant.TOKEN_MEMBER_PRE + appHeader);
        if (StrUtil.isBlank(v)) {
            ResponseUtil.out(response, ResponseUtil.resultMap(false, 401, "???????????????????????????????????????"));
            return null;
        }
        TokenMember member = new Gson().fromJson(v, TokenMember.class);
        username = member.getUsername();
        // ??????
        List<GrantedAuthority> authorities = securityUtil.getCurrMemberPerms(username);

        // ????????????????????????
        redisTemplate.set(SecurityConstant.MEMBER_TOKEN + username + ":" + member.getPlatform(), appHeader, appTokenProperties.getTokenExpireTime(), TimeUnit.DAYS);
        redisTemplate.set(SecurityConstant.TOKEN_MEMBER_PRE + appHeader, v, appTokenProperties.getTokenExpireTime(), TimeUnit.DAYS);

        if (StrUtil.isNotBlank(username)) {
            // ???????????? ??????password?????????null
            User principal = new User(username, "", authorities);
            return new UsernamePasswordAuthenticationToken(principal, null, authorities);
        }
        return null;
    }
}

