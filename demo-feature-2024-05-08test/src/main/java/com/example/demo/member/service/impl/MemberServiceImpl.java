package com.example.demo.member.service.impl;

import com.example.demo.components.MailComponents;
import com.example.demo.member.entity.Member;
import com.example.demo.member.exception.MemberNotEmailAuthException;
import com.example.demo.member.model.MemberInput;
import com.example.demo.member.model.ResetPasswordInput;
import com.example.demo.member.repository.MemberRepository;
import com.example.demo.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.demo.DemoApplication.post;

@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MailComponents mailComponents;

    @Override
    public boolean register(MemberInput parameter) {
        Optional<Member> optionalMember = memberRepository.findById(parameter.getUserId());
        if(optionalMember.isPresent()) {
            //해당 Id에 데이터 존재
            return false;
        }

        String encPassword = BCrypt.hashpw(parameter.getPassword(), BCrypt.gensalt());

        String uuid = UUID.randomUUID().toString();

        Member member = Member.builder()
                .userId(parameter.getUserId())
                .userName(parameter.getUserName())
                .phone(parameter.getPhone())
                .password(encPassword)
                .regDt(LocalDateTime.now())
                .emailAuthYn(false)
                .emailAuthKey(uuid)
                .build();

        memberRepository.save(member);

        String email = parameter.getUserId();
        String subject = "demo 사이트 가입을 축하드립니다.";
        String text = "<p> demo 사이트 가입을 축하드립니다.</p> <p>아래 링크를 클릭하셔서 가입을 완료하세요.</p>"
                + "<div><a target='_blank' href='http://localhost:8080/member/email-auth?id=" + uuid + "'> 가입완료 </a></div>";

        mailComponents.sendMail(email, subject, text);

        return true;
    }

    @Override
    public boolean changeName(String name, String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setUserName(name);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean changePhone(String phone, String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setPhone(phone);

        memberRepository.save(member);

        return true;
    }


    @Override
    public boolean emailAuth(String uuid) {
        Optional<Member> optionalMember = memberRepository.findByEmailAuthKey(uuid);//null이 가능한 member안의 인스턴스를 optionalMember객체에연결하고 memberrepository에서 emailauthkey를 찾아서연결
        if (!optionalMember.isPresent()) {
            return false;
        }

        Member member = optionalMember.get();

        if(member.isEmailAuthYn()) {
            return false;
        }

        member.setEmailAuthYn(true);
        member.setEmailAuthDt(LocalDateTime.now());
        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean sendResetPassword(ResetPasswordInput parameter) {
        Optional<Member> optionalMember = memberRepository.findByUserIdAndUserName(parameter.getUserId(), parameter.getUserName());
        if (!optionalMember.isPresent()) {
            throw new UsernameNotFoundException("회원 정보가 존재하지않습니다.");
        }

        Member member = optionalMember.get();

        String uuid = UUID.randomUUID().toString();

        member.setResetPasswordKey(uuid);
        member.setResetPasswordLimitDt(LocalDateTime.now().plusDays(1));
        memberRepository.save(member);

        String email = parameter.getUserId();
        String subject = "[demo] 비밀번호 초기화 메일 입니다.";
        String text = "<p> demo 비밀번호 초기화 메일입니다.</p> <p>아래 링크를 클릭하셔서 비밀번호를 초기화 해주세요.</p>" +
                "<div><a target='_blank' href='http://localhost:8080/member/reset/password?id=" + uuid + "'> 비밀번호 초기화 링크 </a></div>";
        mailComponents.sendMail(email, subject, text);

        return true;
    }

    @Override
    public boolean resetPassword(String uuid, String password) {
        Optional<Member> optionalMember = memberRepository.findByResetPasswordKey(uuid);
        if (!optionalMember.isPresent()) {
            throw new UsernameNotFoundException("회원 정보가 존재하지않습니다.");
        }

        Member member = optionalMember.get();

        //초기화 날짜가 유효한지 체크
        if(member.getResetPasswordLimitDt() == null) {
            throw new RuntimeException("유효한 날짜가 아닙니다.");
        }

        if(member.getResetPasswordLimitDt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("유효한 날짜가 아닙니다.");
        }

        String encPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        member.setPassword(encPassword);
        member.setResetPasswordKey("");
        member.setResetPasswordLimitDt(null);
        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean checkResetPassword(String uuid) {

        Optional<Member> optionalMember = memberRepository.findByResetPasswordKey(uuid);
        if (!optionalMember.isPresent()) {
            return false;
        }

        Member member = optionalMember.get();

        //초기화 날짜가 유효한지 체크
        if(member.getResetPasswordLimitDt() == null) {
            throw new RuntimeException("유효한 날짜가 아닙니다.");
        }

        if(member.getResetPasswordLimitDt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("유효한 날짜가 아닙니다.");
        }

        return true;
    }


    @Override
    public String NaverApiResponse(String query1, String query2, String query3, String query4, String query5, String date1, String date2, String timeunit, String coverage, String gender, String[] age) throws JSONException {
        String clientId = "yeRsNjkDl0PmHo3i09r1"; // 애플리케이션 클라이언트 아이디
        String clientSecret = "iqRygcj9AF"; // 애플리케이션 클라이언트 시크릿

        String apiUrl = "https://openapi.naver.com/v1/datalab/search";

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);
        requestHeaders.put("Content-Type", "application/json");

        String agesString = "";
        if (age != null) {
            agesString = "[" + Arrays.stream(age)
                    .map(a -> "\"" + a + "\"")
                    .collect(Collectors.joining(",")) + "]";
        }

        String requestBody = "{\"startDate\":\"" + date1 + "\"," +
                "\"endDate\":\"" + date2 + "\"," +
                "\"timeUnit\":\"" +  timeunit + "\"," +
                "\"keywordGroups\":" + "[{\"groupName\":\"" + query1 + "\"," + "\"keywords\":[\"" + query1 + "\"]}" +
                (query2.isBlank() ? "]," : ",{\"groupName\":\"" + query2 + "\"," + "\"keywords\":[\"" + query2 + "\"]}") +
                (query2.isBlank() ? "" : (query3.isBlank() ? "]," : ",{\"groupName\":\"" + query3 + "\"," + "\"keywords\":[\"" + query3 + "\"]}")) +
                (query3.isBlank() ? "" : (query4.isBlank() ? "]," : ",{\"groupName\":\"" + query4 + "\"," + "\"keywords\":[\"" + query4 + "\"]}")) +
                (query4.isBlank() ? "" : (query5.isBlank() ? "]," : ",{\"groupName\":\"" + query5 + "\"," + "\"keywords\":[\"" + query5 + "\"]}],")) +
                "\"device\":\"" + coverage + "\"," +
                "\"ages\":" + agesString + "," +
                "\"gender\":\"" + gender + "\"}";

        String responseBody = post(apiUrl, requestHeaders, requestBody);

        return responseBody;
    }

    @Override
    public ResultMaps parseJson(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray resultsArray = jsonObject.getJSONArray("results");

        Map<String, List<String>> periodsMap = new HashMap<>();
        Map<String, List<Double>> ratiosMap = new HashMap<>();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject result = resultsArray.getJSONObject(i);
            String title = result.getString("title");
            JSONArray dataArray = result.getJSONArray("data");

            List<String> periods = new ArrayList<>();
            List<Double> ratios = new ArrayList<>();

            for (int j = 0; j < dataArray.length(); j++) {
                JSONObject dataPoint = dataArray.getJSONObject(j);
                periods.add(dataPoint.getString("period"));
                ratios.add(dataPoint.getDouble("ratio"));
            }

            periodsMap.put(title, periods);
            ratiosMap.put(title, ratios);
        }

        return new ResultMaps(periodsMap, ratiosMap);
    }

    @Override
    public boolean setDbFavoriteURL(String url, String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        if (member.getFavorite1()==null){
            member.setFavorite1(url);
        } else if (member.getFavorite2()==null) {
            member.setFavorite2(url);
        } else if (member.getFavorite3()==null) {
            member.setFavorite3(url);
        } else if (member.getFavorite4()==null) {
            member.setFavorite4(url);
        } else if (member.getFavorite5()==null) {
            member.setFavorite5(url);
        }

        memberRepository.save(member);

        return true;
    }

    @Override
    public ArrayList<String> getDbFavoriteURL(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);
        Member member = optionalMember.get();

        ArrayList<String> favoriteURL = new ArrayList<>();

        if(member.getFavorite1()!=null){
            favoriteURL.add(member.getFavorite1());
        }else {
            favoriteURL.add("");
        }
        if (member.getFavorite2()!=null){
            favoriteURL.add(member.getFavorite2());
        }else {
            favoriteURL.add("");
        }
        if (member.getFavorite3()!=null) {
            favoriteURL.add(member.getFavorite3());
        }else {
            favoriteURL.add("");
        }
        if (member.getFavorite4()!=null) {
            favoriteURL.add(member.getFavorite4());
        }else {
            favoriteURL.add("");
        }
        if (member.getFavorite5()!=null) {
            favoriteURL.add(member.getFavorite5());
        }else {
            favoriteURL.add("");
        }

        return favoriteURL;
    }

    @Override
    public boolean setDbCalFavoriteURL(String url, String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        if (member.getCal_favorite1()==null){
            member.setCal_favorite1(url);
        } else if (member.getCal_favorite2()==null) {
            member.setCal_favorite2(url);
        } else if (member.getCal_favorite3()==null) {
            member.setCal_favorite3(url);
        } else if (member.getCal_favorite4()==null) {
            member.setCal_favorite4(url);
        } else if (member.getCal_favorite5()==null) {
            member.setCal_favorite5(url);
        }

        memberRepository.save(member);

        return true;
    }

    @Override
    public ArrayList<String> getDbCalFavoriteURL(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        ArrayList<String> favoriteURL = new ArrayList<>();

        if(member.getCal_favorite1()!=null){
            favoriteURL.add(member.getCal_favorite1());
        }else {
            favoriteURL.add("");
        }
        if (member.getCal_favorite2()!=null){
            favoriteURL.add(member.getCal_favorite2());
        }else {
            favoriteURL.add("");
        }
        if (member.getCal_favorite3()!=null) {
            favoriteURL.add(member.getCal_favorite3());
        }else {
            favoriteURL.add("");
        }
        if (member.getCal_favorite4()!=null) {
            favoriteURL.add(member.getCal_favorite4());
        }else {
            favoriteURL.add("");
        }
        if (member.getCal_favorite5()!=null) {
            favoriteURL.add(member.getCal_favorite5());
        }else {
            favoriteURL.add("");
        }

        return favoriteURL;
    }

    @Override
    public ArrayList<String> extractUrl(String str) {
        ArrayList<String> arr = new ArrayList<>();

        String keyword1 = "query1=";
        String keyword2 = "&query2=";
        String keyword3 = "&query3=";
        String keyword4 = "&query4=";
        String keyword5 = "&query5=";
        String keyword6 = "&date1=";
        String keyword7 = "&date2=";
        String keyword8 = "&select_day_week_month=";
        String keyword9 = "&device=";
        String keyword10 = "&gender=";
        String keyword11 = "&age=";
        String keyword12 = "&date3=";
        String keyword13 = "&date4=";
        String keyword14 = "&select_day_week_month2=";
        String keyword15 = "&device2=";
        String keyword16 = "&gender2=";
        String keyword17 = "&age2=";

        String regexPattern1 = keyword1 + "(.*?)" + keyword2;
        String regexPattern2 = keyword2 + "(.*?)" + keyword3;
        String regexPattern3 = keyword3 + "(.*?)" + keyword4;
        String regexPattern4 = keyword4 + "(.*?)" + keyword5;
        String regexPattern5 = keyword5 + "(.*?)" + keyword6;
        String regexPattern6 = keyword6 + "(.*?)" + keyword7;
        String regexPattern7 = keyword7 + "(.*?)" + keyword8;
        String regexPattern8 = keyword8 + "(.*?)" + keyword9;
        String regexPattern9 = keyword9 + "(.*?)" + keyword10;
        String regexPattern10 = keyword10 + "(.*?)" + keyword11;
        String regexPattern11 = keyword12 + "(.*?)" + keyword13;
        String regexPattern12 = keyword13 + "(.*?)" + keyword14;
        String regexPattern13 = keyword14 + "(.*?)" + keyword15;
        String regexPattern14 = keyword15 + "(.*?)" + keyword16;
        String regexPattern15 = keyword16 + "(.*?)" + keyword17;

        Pattern pattern1 = Pattern.compile(regexPattern1);
        Pattern pattern2 = Pattern.compile(regexPattern2);
        Pattern pattern3 = Pattern.compile(regexPattern3);
        Pattern pattern4 = Pattern.compile(regexPattern4);
        Pattern pattern5 = Pattern.compile(regexPattern5);
        Pattern pattern6 = Pattern.compile(regexPattern6);
        Pattern pattern7 = Pattern.compile(regexPattern7);
        Pattern pattern8 = Pattern.compile(regexPattern8);
        Pattern pattern9 = Pattern.compile(regexPattern9);
        Pattern pattern10 = Pattern.compile(regexPattern10);
        Pattern pattern11 = Pattern.compile(regexPattern11);
        Pattern pattern12 = Pattern.compile(regexPattern12);
        Pattern pattern13 = Pattern.compile(regexPattern13);
        Pattern pattern14 = Pattern.compile(regexPattern14);
        Pattern pattern15 = Pattern.compile(regexPattern15);

        Matcher matcher1 = pattern1.matcher(str);
        Matcher matcher2 = pattern2.matcher(str);
        Matcher matcher3 = pattern3.matcher(str);
        Matcher matcher4 = pattern4.matcher(str);
        Matcher matcher5 = pattern5.matcher(str);
        Matcher matcher6 = pattern6.matcher(str);
        Matcher matcher7 = pattern7.matcher(str);
        Matcher matcher8 = pattern8.matcher(str);
        Matcher matcher9 = pattern9.matcher(str);
        Matcher matcher10 = pattern10.matcher(str);
        Matcher matcher11 = pattern11.matcher(str);
        Matcher matcher12 = pattern12.matcher(str);
        Matcher matcher13 = pattern13.matcher(str);
        Matcher matcher14 = pattern14.matcher(str);
        Matcher matcher15 = pattern15.matcher(str);

        while (matcher1.find()) {
            String extractedContent = matcher1.group(1);
            arr.add(extractedContent);
        }
        while (matcher2.find()) {
            String extractedContent = matcher2.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher3.find()) {
            String extractedContent = matcher3.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher4.find()) {
            String extractedContent = matcher4.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher5.find()) {
            String extractedContent = matcher5.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher6.find()) {
            String extractedContent = matcher6.group(1);
            arr.add(extractedContent);
        }
        while (matcher7.find()) {
            String extractedContent = matcher7.group(1);
            arr.add(extractedContent);
        }
        while (matcher8.find()) {
            String extractedContent = matcher8.group(1);
            if (extractedContent.equals("date")){
                arr.add("일간");
            } else if (extractedContent.equals("week")) {
                arr.add("주간");
            } else if (extractedContent.equals("month")) {
                arr.add("월간");
            }
        }
        while (matcher9.find()) {
            String extractedContent = matcher9.group(1);
            if (extractedContent.equals("")){
                arr.add("전체");
            } else if (extractedContent.equals("mo")) {
                arr.add("모바일");
            } else if (extractedContent.equals("pc")) {
                arr.add("PC");
            }
        }
        while (matcher10.find()) {
            String extractedContent = matcher10.group(1);
            if (extractedContent.equals("")){
                arr.add("전체");
            } else if (extractedContent.equals("f")) {
                arr.add("여성");
            } else if (extractedContent.equals("m")) {
                arr.add("남성");
            }
        }
        while (matcher11.find()) {
            String extractedContent = matcher11.group(1);
            arr.add(extractedContent);
        }
        while (matcher12.find()) {
            String extractedContent = matcher12.group(1);
            arr.add(extractedContent);
        }
        while (matcher13.find()) {
            String extractedContent = matcher13.group(1);
            if (extractedContent.equals("date")){
                arr.add("일간");
            } else if (extractedContent.equals("week")) {
                arr.add("주간");
            } else if (extractedContent.equals("month")) {
                arr.add("월간");
            }
        }
        while (matcher14.find()) {
            String extractedContent = matcher14.group(1);
            if (extractedContent.equals("")){
                arr.add("전체");
            } else if (extractedContent.equals("mo")) {
                arr.add("모바일");
            } else if (extractedContent.equals("pc")) {
                arr.add("PC");
            }
        }
        while (matcher15.find()) {
            String extractedContent = matcher15.group(1);
            if (extractedContent.equals("")){
                arr.add("전체");
            } else if (extractedContent.equals("f")) {
                arr.add("여성");
            } else if (extractedContent.equals("m")) {
                arr.add("남성");
            }
        }

        return arr;
    }

    @Override
    public ArrayList<String> extractCalUrl(String str) {
        ArrayList<String> arr = new ArrayList<>();

        String keyword1 = "query1=";
        String keyword2 = "&query2=";
        String keyword3 = "&query3=";
        String keyword4 = "&query4=";
        String keyword5 = "&query5=";
        String keyword6 = "&date1=";
        String keyword7 = "&date2=";
        String keyword8 = "&select_day_week_month=";
        String keyword9 = "&device=";
        String keyword10 = "&gender=";
        String keyword11 = "&age=";

        String regexPattern1 = keyword1 + "(.*?)" + keyword2;
        String regexPattern2 = keyword2 + "(.*?)" + keyword3;
        String regexPattern3 = keyword3 + "(.*?)" + keyword4;
        String regexPattern4 = keyword4 + "(.*?)" + keyword5;
        String regexPattern5 = keyword5 + "(.*?)" + keyword6;
        String regexPattern6 = keyword6 + "(.*?)" + keyword7;
        String regexPattern7 = keyword7 + "(.*?)" + keyword8;
        String regexPattern8 = keyword8 + "(.*?)" + keyword9;
        String regexPattern9 = keyword9 + "(.*?)" + keyword10;
        String regexPattern10 = keyword10 + "(.*?)" + keyword11;

        Pattern pattern1 = Pattern.compile(regexPattern1);
        Pattern pattern2 = Pattern.compile(regexPattern2);
        Pattern pattern3 = Pattern.compile(regexPattern3);
        Pattern pattern4 = Pattern.compile(regexPattern4);
        Pattern pattern5 = Pattern.compile(regexPattern5);
        Pattern pattern6 = Pattern.compile(regexPattern6);
        Pattern pattern7 = Pattern.compile(regexPattern7);
        Pattern pattern8 = Pattern.compile(regexPattern8);
        Pattern pattern9 = Pattern.compile(regexPattern9);
        Pattern pattern10 = Pattern.compile(regexPattern10);

        Matcher matcher1 = pattern1.matcher(str);
        Matcher matcher2 = pattern2.matcher(str);
        Matcher matcher3 = pattern3.matcher(str);
        Matcher matcher4 = pattern4.matcher(str);
        Matcher matcher5 = pattern5.matcher(str);
        Matcher matcher6 = pattern6.matcher(str);
        Matcher matcher7 = pattern7.matcher(str);
        Matcher matcher8 = pattern8.matcher(str);
        Matcher matcher9 = pattern9.matcher(str);
        Matcher matcher10 = pattern10.matcher(str);

        while (matcher1.find()) {
            String extractedContent = matcher1.group(1);
            arr.add(extractedContent);
        }
        while (matcher2.find()) {
            String extractedContent = matcher2.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher3.find()) {
            String extractedContent = matcher3.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher4.find()) {
            String extractedContent = matcher4.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher5.find()) {
            String extractedContent = matcher5.group(1);
            if (extractedContent.equals("")){
                arr.add(null);
            }else {
                arr.add(extractedContent);
            }
        }
        while (matcher6.find()) {
            String extractedContent = matcher6.group(1);
            arr.add(extractedContent);
        }
        while (matcher7.find()) {
            String extractedContent = matcher7.group(1);
            arr.add(extractedContent);
        }
        while (matcher8.find()) {
            String extractedContent = matcher8.group(1);
            if (extractedContent.equals("date")){
                arr.add("일간");
            } else if (extractedContent.equals("week")) {
                arr.add("주간");
            } else if (extractedContent.equals("month")) {
                arr.add("월간");
            }
        }
        while (matcher9.find()) {
            String extractedContent = matcher9.group(1);
            if (extractedContent.equals("")){
                arr.add("전체");
            } else if (extractedContent.equals("mo")) {
                arr.add("모바일");
            } else if (extractedContent.equals("pc")) {
                arr.add("PC");
            }
        }
        while (matcher10.find()) {
            String extractedContent = matcher10.group(1);
            if (extractedContent.equals("")){
                arr.add("전체");
            } else if (extractedContent.equals("f")) {
                arr.add("여성");
            } else if (extractedContent.equals("m")) {
                arr.add("남성");
            }
        }

        return arr;
    }

    @Override
    public boolean setRemoveAllUrl(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setFavorite1(null);
        member.setFavorite2(null);
        member.setFavorite3(null);
        member.setFavorite4(null);
        member.setFavorite5(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveUrl1(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setFavorite1(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveUrl2(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setFavorite2(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveUrl3(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setFavorite3(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveUrl4(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setFavorite4(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveUrl5(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setFavorite5(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveAllCalUrl(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setCal_favorite1(null);
        member.setCal_favorite2(null);
        member.setCal_favorite3(null);
        member.setCal_favorite4(null);
        member.setCal_favorite5(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveCalUrl1(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setCal_favorite1(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveCalUrl2(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setCal_favorite2(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveCalUrl3(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setCal_favorite3(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveCalUrl4(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setCal_favorite4(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean setRemoveCalUrl5(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();

        member.setCal_favorite5(null);

        memberRepository.save(member);

        return true;
    }

    @Override
    public ArrayList<String> getAge1(String str) {
        String[] arr = new String[13];
        String[] arr3 = new String[13];
        ArrayList<String> arrayList = new ArrayList<>();

        arr = str.split("&age=");
        arr3 = arr[arr.length-1].split("&date3=");

        for (int i = 1; i < arr.length; i++) {
            if (i!=arr.length-1){
                arrayList.add(arr[i]);
            }else if (!arr3[0].equals("")){
                arrayList.add(arr3[0]);
            }
        }

        return arrayList;
    }

    @Override
    public ArrayList<String> getAge(String str) {
        String[] arr = new String[10];
        ArrayList<String> arrayList = new ArrayList<>();

        arr = str.split("&age=");

        for (int i = 1; i < arr.length; i++) {
            arrayList.add(arr[i]);
        }
        return arrayList;
    }

    @Override
    public ArrayList<String> getAge2(String str) {
        String[] arr = new String[13];
        ArrayList<String> arrayList = new ArrayList<>();

        arr = str.split("&age=");

        String[] arr2 = new String[13];

        arr2 = arr[arr.length-1].split("&age2=");

        for (int i = 1; i < arr2.length; i++) {
            arrayList.add(arr2[i]);
        }

        return arrayList;
    }

    @Override
    public String getName(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();
        String name = "";
        name = member.getUserName();
        return name;
    }

    @Override
    public String getPhone(String username) {
        Optional<Member> optionalMember = memberRepository.findById(username);

        Member member = optionalMember.get();
        String phone = "";
        phone = member.getPhone();
        return phone;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<Member> optionalMember = memberRepository.findById(username);
        if (!optionalMember.isPresent()) {
            throw new UsernameNotFoundException("회원 정보가 존재하지않습니다.");
        }

        Member member = optionalMember.get();

        if(!member.isEmailAuthYn()) { // 메일 활성화 안되었을 때 오류 처리
            throw new MemberNotEmailAuthException("이메일 활성화 이후에 로그인해주세요.");
        }

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (member.isAdminYn()) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new User(member.getUserId(), member.getPassword(), grantedAuthorities);
    }
}