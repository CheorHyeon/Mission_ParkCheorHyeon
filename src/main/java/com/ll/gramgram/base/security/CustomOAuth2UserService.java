package com.ll.gramgram.base.security;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
// 소설로그인 시 실행
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final Rq rq;
    private final MemberService memberService;
    private final InstaMemberService instaMemberService;

    @Override
    @Transactional
    // loadUser : 소셜 로그인 요청 처리하는 메서드(loadUser) 오버라이드
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 로그인 서비스 제공자의 이름을 가져옴(kakao, google 등)
        String providerTypeCode = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        if (providerTypeCode.equals("INSTAGRAM")) {
            if (rq.isLogout()) {
                throw new OAuth2AuthenticationException("로그인 후 이용해주세요.");
            }
            // 사용자 정보 엔드포인트에서 사용자 정보를 가져옴
            String userInfoUri = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();
            userInfoUri = userInfoUri.replace("{access-token}", userRequest.getAccessToken().getTokenValue());
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, Map.class);
            
            Map<String, String> userAttributes = response.getBody();

            String gender = rq.getSessionAttr("connectByApi__gender", "W");
            rq.removeSessionAttr("connectByApi__gender");

            instaMemberService.connect(rq.getMember(), gender, userAttributes.get("id"), userAttributes.get("username"), userRequest.getAccessToken().getTokenValue());

            Member member = rq.getMember();
            return new CustomOAuth2User(member.getUsername(), member.getPassword(), member.getGrantedAuthorities());
        }

        // OAuth2UserRequest 객체를 인자로 받아, 해당 객체에 포함된 AccessToken과 OAuth2UserRequest 정보를 기반으로 OAuth2 프로바이더로부터 사용자 정보를 가져옵니다.
        // 이 때 가져온 정보를 OAuth2User 타입의 객체로 반환합니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // 사용자의 ID 가져옴 : naver의 경우 맵형태로 가져와져서 파싱 작업 필요
        String oauthId = switch (providerTypeCode) {
            case "NAVER" -> ((Map<String, String>) oAuth2User.getAttributes().get("response")).get("id");
            default -> oAuth2User.getName();
        };

        String username = providerTypeCode + "__%s".formatted(oauthId);
        // 사용자 정보를 가져옴
        Member member = memberService.whenSocialLogin(providerTypeCode, username).getData();
        // User객체 반환(로그인 처리)
        return new CustomOAuth2User(member.getUsername(), member.getPassword(), member.getGrantedAuthorities());
    }
}

class CustomOAuth2User extends User implements OAuth2User {

    public CustomOAuth2User(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        // User 클래스를 상속받아 사용자의 username, password, 권한 정보를 저장함
        super(username, password, authorities);
    }

    // OAuth2User 인터페이스를 상속받아, 사용자 정보를 반환함
    // 원래 OAuth2 프로바이더에서 가져온 사용자 정보를 맵형태로 반환하는 메서드
    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }
    // OAuth2User 인터페이스를 상속받아, 사용자의 이름을 반환함
    @Override
    public String getName() {
        return getUsername();
    }
}