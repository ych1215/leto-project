package com.example.demo.member.service;

import com.example.demo.admin.dto.MemberDto;
import com.example.demo.admin.model.MemberParam;
import com.example.demo.member.model.MemberInput;
import com.example.demo.member.model.ResetPasswordInput;
import com.example.demo.member.service.impl.ResultMaps;
import org.json.JSONException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface MemberService extends UserDetailsService {

    boolean register(MemberInput parameter);

    //uuid에 해당하는 계정을 활성화함.
    boolean emailAuth(String uuid);

    /* 입력한 이메일로 비밀번호 초기화 정보를 전송 */
    boolean sendResetPassword(ResetPasswordInput parameter);

    /*
    -입력 받은 uuid에 대해서 password로 초기화함
    */
    boolean resetPassword(String id, String password);

    /*
     *입력받은 uuid값이 유효한지 확인
     */
    boolean checkResetPassword(String uuid);

    /*회원의 목록을 리턴함(관리자페이지에서만 사용가능)*/
    List<MemberDto> list(MemberParam parameter);

    /*회원 상세 정보*/
    MemberDto detail(String userId);
    String NaverApiResponse(String query1, String query2, String query3, String query4, String query5, String year, String  month1, String day1, String year2, String month2, String day2, String timeunit, String coverage, String gender, String[] age) throws JSONException;
    ResultMaps parseJson(String jsonString) throws JSONException;
    boolean setDbFavoriteURL(String url, String username);
    ArrayList<String> getDbFavoriteURL(String username);
    boolean setDbCalFavoriteURL(String url, String username);
    ArrayList<String> getDbCalFavoriteURL(String username);

    ArrayList<String> extractUrl(String str);
    ArrayList<String> extractCalUrl(String str);

    boolean setRemoveAllUrl(String username);

    boolean setRemoveUrl1(String username);

    boolean setRemoveUrl2(String username);

    boolean setRemoveUrl3(String username);

    boolean setRemoveUrl4(String username);

    boolean setRemoveUrl5(String username);


    boolean setRemoveAllCalUrl(String username);

    boolean setRemoveCalUrl1(String username);

    boolean setRemoveCalUrl2(String username);

    boolean setRemoveCalUrl3(String username);

    boolean setRemoveCalUrl4(String username);

    boolean setRemoveCalUrl5(String username);

    ArrayList<String> getAge1(String str);
    ArrayList<String> getAge2(String str);

    boolean changeName(String name, String username);
    boolean changePhone(String phone, String username);

    String getName(String username);
    String getPhone(String username);

}