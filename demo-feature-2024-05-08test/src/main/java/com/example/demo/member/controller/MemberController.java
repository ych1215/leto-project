package com.example.demo.member.controller;

import com.example.demo.DataMining.NaverParsing;
import com.example.demo.member.model.MemberInput;
import com.example.demo.member.model.ResetPasswordInput;
import com.example.demo.member.service.MemberService;
import com.example.demo.member.service.impl.ResultMaps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.eclipse.tags.shaded.org.apache.xpath.operations.Mod;
import org.json.simple.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
public class MemberController {
    private final MemberService memberService;
    private JSONObject[] resultsArray;

    @RequestMapping("/login")
    public String login() {
        return "member/login";
    }

    @GetMapping("/member/register")
    public String register() {
        return "member/register";
    }

    @PostMapping("/member/register")
    public String registerSubmit(Model model, MemberInput parameter) {
        boolean result = memberService.register(parameter);

        model.addAttribute("result", result);

        return "member/register_complete";
        //서비스의 비지니스로직을 활용해  멤버인풋 파라미터를 받아와 데이터베이스에 정상적으로 저장되면 회원가입이 완료되었습니다 아니면 실패하였습니다.
        //그리고 이메일을 보낸다
    }

    @GetMapping("/calendar")
    public String calendar(Principal principal,
                           @RequestParam(name = "query1", required = false, defaultValue = "") String query1,
                           @RequestParam(name = "query2", required = false, defaultValue = "") String query2,
                           @RequestParam(name = "query3", required = false, defaultValue = "") String query3,
                           @RequestParam(name = "query4", required = false, defaultValue = "") String query4,
                           @RequestParam(name = "query5", required = false, defaultValue = "") String query5,
                           @RequestParam(name = "year", required = false, defaultValue = "") String year1,
                           @RequestParam(name = "month", required = false, defaultValue = "") String month1,
                           @RequestParam(name = "day", required = false, defaultValue = "") String day1,
                           @RequestParam(name = "year2", required = false, defaultValue = "") String year2,
                           @RequestParam(name = "month2", required = false, defaultValue = "") String month2,
                           @RequestParam(name = "day2", required = false, defaultValue = "") String day2,
                           @RequestParam(name = "select_day_week_month", required = false, defaultValue = "") String timeunit,
                           @RequestParam(name = "device", required = false, defaultValue = "") String coverage,
                           @RequestParam(name = "gender", required = false, defaultValue = "") String gender,
                           @RequestParam(name = "age", required = false, defaultValue = "") String[] age,
                           Model model) throws JSONException {
        String username = principal.getName();
        model.addAttribute("username", username);

        if (!query1.equals("")){
            String jsonString = memberService.NaverApiResponse(query1, query2, query3, query4, query5, year1, month1, day1, year2, month2, day2, timeunit, coverage, gender, age);
            ResultMaps result1Maps = memberService.parseJson(jsonString);
            int i = result1Maps.getPeriodsMap().size();
            ArrayList<String> titles = new ArrayList<>();
            for (String title : result1Maps.getPeriodsMap().keySet()) {
                titles.add(title);
                if (i==result1Maps.getPeriodsMap().size()){
                    model.addAttribute("years", result1Maps.getPeriodsMap().get(title));
                }
                model.addAttribute("value"+i, result1Maps.getRatiosMap().get(title));
                i--;
            }
            model.addAttribute("title", titles);
        }

        String favoriteURL = "";

        for (int i = 0; i < memberService.getDbCalFavoriteURL(username).size(); i++) {
            favoriteURL = memberService.getDbCalFavoriteURL(username).get(i);
            model.addAttribute("url"+(i+1), favoriteURL);
        }

        boolean isTrue1 = true;
        boolean isTrue2 = true;
        boolean isTrue3 = true;
        boolean isTrue4 = true;
        boolean isTrue5 = true;
        boolean isTrueAll = true;

        for (int i = 0; i < memberService.getDbCalFavoriteURL(username).size(); i++) {
            favoriteURL = memberService.getDbCalFavoriteURL(username).get(i);

            if (i==0&&favoriteURL==""){
                isTrue1 = false;
            }
            if (i==1&&favoriteURL==""){
                isTrue2 = false;
            }
            if (i==2&&favoriteURL==""){
                isTrue3 = false;
            }
            if (i==3&&favoriteURL==""){
                isTrue4 = false;
            }
            if (i==4&&favoriteURL==""){
                isTrue5 = false;
            }
            if (isTrue1==true&&isTrue2==true&&isTrue3==true&&isTrue4==true&&isTrue5==true){
                model.addAttribute("isTrueAll", isTrueAll);
            } else {
                isTrueAll = false;
                model.addAttribute("isTrueAll", isTrueAll);
            }
        }

        return "calendar";
    }

    @GetMapping("/mypage")
    public String mypage(Principal principal,
                         Model model) {
        String username = principal.getName();
        String user_name = memberService.getName(username);
        String phone = memberService.getPhone(username);
        model.addAttribute("username", username);
        model.addAttribute("name", user_name);
        model.addAttribute("phone", phone);

        return "mypage";
    }

    @GetMapping("/changeName")
    public String changeName(Principal principal, Model model){
        String username = principal.getName();
        String user_name = memberService.getName(username);

        model.addAttribute("username", username);
        model.addAttribute("name", user_name);

        return "changeName";
    }

    @PostMapping("/changeName")
    public String changeNamePost(Principal principal,
                                 @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                 Model model){
        String username = principal.getName();
        String user_name = memberService.getName(username);

        if (!name.equals("")){
            memberService.changeName(name, username);
        }
        System.out.println(name);
        model.addAttribute("username", username);
        model.addAttribute("name", user_name);

        return "changeName";
    }

    @GetMapping("/changePhone")
    public String changePhone(Principal principal,
                              Model model){
        String username = principal.getName();
        String phone = memberService.getPhone(username);
        model.addAttribute("username", username);
        model.addAttribute("phone", phone);
        return "changePhone";
    }

    @PostMapping("/changePhone")
    public String changePhone(Principal principal,
                              @RequestParam(name = "phone", required = false, defaultValue = "") String phone,
                              Model model){
        String username = principal.getName();
        String rphone = memberService.getPhone(username);

        if (!phone.equals("")){
            memberService.changePhone(phone, username);
        }
        model.addAttribute("username", username);
        model.addAttribute("phone", rphone);

        return "changePhone";
    }

    @GetMapping("/wordcloud")
    public String wordcloud(Principal principal,
                            Model model){
        String username = principal.getName();
        model.addAttribute("username", username);

        return "wordcloud";
    }


    @RequestMapping(value = "/wordCloud", method = RequestMethod.GET)
    public void wordCloud(HttpServletResponse res, HttpServletRequest req,
                          Principal principal,
                          Model model) throws IOException {
        String username = principal.getName();
        model.addAttribute("username", username);

        // 웹에서 get으로 요청할때 보내온 파라미터 중 word 파라미터를 가져옴
        String word = req.getParameter("word");

        // NaverParsing 클래스의 parsingData 를 실행하고 겨과를 HashMap 로 저장함
        // 이때 파라미터로 웽에서 가져온 word 를 사용
        HashMap<String, Integer> crawlerData = new NaverParsing().parsingData(word);

        // 데이터 저장을 위한 json array
        JSONArray jsonArray = new JSONArray();

        for(String list : crawlerData.keySet()){
            JSONObject informationObject = new JSONObject();
            // JsonArray 에 저장하기 위해서 값을 하나씩 json 형태로 가져와서 x 에 key를 담고 , value  에는 value을 저장함
            // 이때 anychart 의 경우 x 와 value 를 사용하지만
            // JQcloud 의 경우 text 와 weight 를 사용한다.

            // 이후 다시 array 에 담기
            informationObject.put("x", list);
            informationObject.put("value", crawlerData.get(list));

            jsonArray.add(informationObject);
        }

        // 전달하는 값의 타입을 application/json;charset=utf-8 로 하고(한글을 정상적으로 출력하기 위함)
        // printwriter 과 prrint 를 사용하여 값을 response 로 값을 전달함
        // 이때 toJSONString 로 전당하는데 이는 추후 Jsonparsing 을 원활하게 하기 위해서
        // pw 로 값을 전달하면 값이 response body 에 들어가서 보내짐
        res.setContentType("application/json;charset=utf-8");
        PrintWriter pw = res.getWriter();
        pw.print(jsonArray.toJSONString());
        //System.out.println(jsonArray.toJSONString());

    }

    @GetMapping("/member/email-auth")
    public String emailAuth(Model model, HttpServletRequest request) {

        String uuid = request.getParameter("id");

        boolean result = memberService.emailAuth(uuid);
        model.addAttribute("result",result);

        return "member/email_auth";
        //받은 이메일에 가입완료를 클릭하면 정해진 링크로 이동하게되는데 그링크에 id가 랜덤으로 지정되어 보내지게된다.
        //거기서 id를 서비스의 비지니스 로직(emailAuth)을 활용해 boolean 타입으로 반환한다.
        //email_auth.html에서 result의 boolean타입을 활용해 뷰를 출력한다.
    }

    @GetMapping("/favorite_url")
    public String favorite_url( Model model,
                                @RequestParam(name = "url", required = false, defaultValue = "") String url,
                                Principal principal) {
        String userName = principal.getName();
        model.addAttribute("username", userName);

        String favoriteURL = "";


        if (!url.equals(memberService.getDbFavoriteURL(userName).get(0))&&!url.equals(memberService.getDbFavoriteURL(userName).get(1))&&!url.equals(memberService.getDbFavoriteURL(userName).get(2))&&!url.equals(memberService.getDbFavoriteURL(userName).get(3))&&!url.equals(memberService.getDbFavoriteURL(userName).get(4))){
            memberService.setDbFavoriteURL(url,userName);
        }

        boolean isTrue1 = true;
        boolean isTrue2 = true;
        boolean isTrue3 = true;
        boolean isTrue4 = true;
        boolean isTrue5 = true;

        for (int i = 0; i < memberService.getDbFavoriteURL(userName).size(); i++) {
            favoriteURL = memberService.getDbFavoriteURL(userName).get(i);
            ArrayList<String> arrayList = memberService.extractUrl(favoriteURL);
            ArrayList<String> arrayList2 = memberService.getAge1(favoriteURL);
            ArrayList<String> arrayList3 = memberService.getAge2(favoriteURL);

            if (arrayList2.size()==0){
                arrayList2.add("전체");
            }

            if (arrayList3.size()==0){
                arrayList3.add("전체");
            }

            if (favoriteURL!=""){
                for (int j = 0; j < arrayList2.size(); j++) {
                    if (arrayList2.get(j).equals("1")){
                        arrayList2.set(j,"~12");
                    } else if (arrayList2.get(j).equals("2")) {
                        arrayList2.set(j,"13~18");
                    } else if (arrayList2.get(j).equals("3")) {
                        arrayList2.set(j,"19~24");
                    } else if (arrayList2.get(j).equals("4")) {
                        arrayList2.set(j,"25~29");
                    } else if (arrayList2.get(j).equals("5")) {
                        arrayList2.set(j,"30~34");
                    } else if (arrayList2.get(j).equals("6")) {
                        arrayList2.set(j,"35~39");
                    } else if (arrayList2.get(j).equals("7")) {
                        arrayList2.set(j,"40~44");
                    } else if (arrayList2.get(j).equals("8")) {
                        arrayList2.set(j,"45~49");
                    } else if (arrayList2.get(j).equals("9")) {
                        arrayList2.set(j,"50~54");
                    } else if (arrayList2.get(j).equals("10")) {
                        arrayList2.set(j,"54~59");
                    } else if (arrayList2.get(j).equals("11")) {
                        arrayList2.set(j,"60~");
                    }
                }
                for (int j = 0; j < arrayList3.size(); j++) {
                    if (arrayList3.get(j).equals("1")){
                        arrayList3.set(j,"~12");
                    } else if (arrayList3.get(j).equals("2")) {
                        arrayList3.set(j,"13~18");
                    } else if (arrayList3.get(j).equals("3")) {
                        arrayList3.set(j,"19~24");
                    } else if (arrayList3.get(j).equals("4")) {
                        arrayList3.set(j,"25~29");
                    } else if (arrayList3.get(j).equals("5")) {
                        arrayList3.set(j,"30~34");
                    } else if (arrayList3.get(j).equals("6")) {
                        arrayList3.set(j,"35~39");
                    } else if (arrayList3.get(j).equals("7")) {
                        arrayList3.set(j,"40~44");
                    } else if (arrayList3.get(j).equals("8")) {
                        arrayList3.set(j,"45~49");
                    } else if (arrayList3.get(j).equals("9")) {
                        arrayList3.set(j,"50~54");
                    } else if (arrayList3.get(j).equals("10")) {
                        arrayList3.set(j,"54~59");
                    } else if (arrayList3.get(j).equals("11")) {
                        arrayList3.set(j,"60~");
                    }
                }
            }

            if (i == 0&&favoriteURL!="") {
                model.addAttribute("url1", favoriteURL);
                model.addAttribute("url1query1", arrayList.get(0));
                model.addAttribute("url1query2", arrayList.get(1));
                model.addAttribute("url1query3", arrayList.get(2));
                model.addAttribute("url1query4", arrayList.get(3));
                model.addAttribute("url1query5", arrayList.get(4));
                model.addAttribute("url1year1", arrayList.get(5));
                model.addAttribute("url1month1", arrayList.get(6));
                model.addAttribute("url1day1", arrayList.get(7));
                model.addAttribute("url1year2", arrayList.get(8));
                model.addAttribute("url1month2", arrayList.get(9));
                model.addAttribute("url1day2", arrayList.get(10));
                model.addAttribute("url1select_day_week_month", arrayList.get(11));
                model.addAttribute("url1device", arrayList.get(12));
                model.addAttribute("url1gender", arrayList.get(13));
                model.addAttribute("url1age1", arrayList2);
                model.addAttribute("url1year3", arrayList.get(14));
                model.addAttribute("url1month3", arrayList.get(15));
                model.addAttribute("url1day3", arrayList.get(16));
                model.addAttribute("url1year4", arrayList.get(17));
                model.addAttribute("url1month4", arrayList.get(18));
                model.addAttribute("url1day4", arrayList.get(19));
                model.addAttribute("url1select_day_week_month2", arrayList.get(20));
                model.addAttribute("url1device2", arrayList.get(21));
                model.addAttribute("url1gender2", arrayList.get(22));
                model.addAttribute("url1age2", arrayList3);
                model.addAttribute("isTrue1", isTrue1);
            } else if (i==0&&favoriteURL==""){
                isTrue1 = false;
                model.addAttribute("isTrue1", isTrue1);
            }
            if (i == 1&&favoriteURL!="") {
                model.addAttribute("url2", favoriteURL);
                model.addAttribute("url2query1", arrayList.get(0));
                model.addAttribute("url2query2", arrayList.get(1));
                model.addAttribute("url2query3", arrayList.get(2));
                model.addAttribute("url2query4", arrayList.get(3));
                model.addAttribute("url2query5", arrayList.get(4));
                model.addAttribute("url2year1", arrayList.get(5));
                model.addAttribute("url2month1", arrayList.get(6));
                model.addAttribute("url2day1", arrayList.get(7));
                model.addAttribute("url2year2", arrayList.get(8));
                model.addAttribute("url2month2", arrayList.get(9));
                model.addAttribute("url2day2", arrayList.get(10));
                model.addAttribute("url2select_day_week_month", arrayList.get(11));
                model.addAttribute("url2device", arrayList.get(12));
                model.addAttribute("url2gender", arrayList.get(13));
                model.addAttribute("url2age1", arrayList2);
                model.addAttribute("url2year3", arrayList.get(14));
                model.addAttribute("url2month3", arrayList.get(15));
                model.addAttribute("url2day3", arrayList.get(16));
                model.addAttribute("url2year4", arrayList.get(17));
                model.addAttribute("url2month4", arrayList.get(18));
                model.addAttribute("url2day4", arrayList.get(19));
                model.addAttribute("url2select_day_week_month2", arrayList.get(20));
                model.addAttribute("url2device2", arrayList.get(21));
                model.addAttribute("url2gender2", arrayList.get(22));
                model.addAttribute("url2age2", arrayList3);
                model.addAttribute("isTrue2", isTrue2);
            } else if (i==1&&favoriteURL==""){
                isTrue2 = false;
                model.addAttribute("isTrue2", isTrue2);
            }
            if (i == 2&&favoriteURL!="") {
                model.addAttribute("url3", favoriteURL);
                model.addAttribute("url3query1", arrayList.get(0));
                model.addAttribute("url3query2", arrayList.get(1));
                model.addAttribute("url3query3", arrayList.get(2));
                model.addAttribute("url3query4", arrayList.get(3));
                model.addAttribute("url3query5", arrayList.get(4));
                model.addAttribute("url3year1", arrayList.get(5));
                model.addAttribute("url3month1", arrayList.get(6));
                model.addAttribute("url3day1", arrayList.get(7));
                model.addAttribute("url3year2", arrayList.get(8));
                model.addAttribute("url3month2", arrayList.get(9));
                model.addAttribute("url3day2", arrayList.get(10));
                model.addAttribute("url3select_day_week_month", arrayList.get(11));
                model.addAttribute("url3device", arrayList.get(12));
                model.addAttribute("url3gender", arrayList.get(13));
                model.addAttribute("url3age1", arrayList2);
                model.addAttribute("url3year3", arrayList.get(14));
                model.addAttribute("url3month3", arrayList.get(15));
                model.addAttribute("url3day3", arrayList.get(16));
                model.addAttribute("url3year4", arrayList.get(17));
                model.addAttribute("url3month4", arrayList.get(18));
                model.addAttribute("url3day4", arrayList.get(19));
                model.addAttribute("url3select_day_week_month2", arrayList.get(20));
                model.addAttribute("url3device2", arrayList.get(21));
                model.addAttribute("url3gender2", arrayList.get(22));
                model.addAttribute("url3age2", arrayList3);
                model.addAttribute("isTrue3", isTrue3);
            } else if (i==2&&favoriteURL==""){
                isTrue3 = false;
                model.addAttribute("isTrue3", isTrue3);
            }
            if (i == 3&&favoriteURL!="") {
                model.addAttribute("url4", favoriteURL);
                model.addAttribute("url4query1", arrayList.get(0));
                model.addAttribute("url4query2", arrayList.get(1));
                model.addAttribute("url4query3", arrayList.get(2));
                model.addAttribute("url4query4", arrayList.get(3));
                model.addAttribute("url4query5", arrayList.get(4));
                model.addAttribute("url4year1", arrayList.get(5));
                model.addAttribute("url4month1", arrayList.get(6));
                model.addAttribute("url4day1", arrayList.get(7));
                model.addAttribute("url4year2", arrayList.get(8));
                model.addAttribute("url4month2", arrayList.get(9));
                model.addAttribute("url4day2", arrayList.get(10));
                model.addAttribute("url4select_day_week_month", arrayList.get(11));
                model.addAttribute("url4device", arrayList.get(12));
                model.addAttribute("url4gender", arrayList.get(13));
                model.addAttribute("url4age1", arrayList2);
                model.addAttribute("url4year3", arrayList.get(14));
                model.addAttribute("url4month3", arrayList.get(15));
                model.addAttribute("url4day3", arrayList.get(16));
                model.addAttribute("url4year4", arrayList.get(17));
                model.addAttribute("url4month4", arrayList.get(18));
                model.addAttribute("url4day4", arrayList.get(19));
                model.addAttribute("url4select_day_week_month2", arrayList.get(20));
                model.addAttribute("url4device2", arrayList.get(21));
                model.addAttribute("url4gender2", arrayList.get(22));
                model.addAttribute("url4age2", arrayList3);
                model.addAttribute("isTrue4", isTrue4);
            } else if (i==3&&favoriteURL==""){
                isTrue4 = false;
                model.addAttribute("isTrue4", isTrue4);
            }
            if (i == 4&&favoriteURL!="") {
                model.addAttribute("url5", favoriteURL);
                model.addAttribute("url5query1", arrayList.get(0));
                model.addAttribute("url5query2", arrayList.get(1));
                model.addAttribute("url5query3", arrayList.get(2));
                model.addAttribute("url5query4", arrayList.get(3));
                model.addAttribute("url5query5", arrayList.get(4));
                model.addAttribute("url5year1", arrayList.get(5));
                model.addAttribute("url5month1", arrayList.get(6));
                model.addAttribute("url5day1", arrayList.get(7));
                model.addAttribute("url5year2", arrayList.get(8));
                model.addAttribute("url5month2", arrayList.get(9));
                model.addAttribute("url5day2", arrayList.get(10));
                model.addAttribute("url5select_day_week_month", arrayList.get(11));
                model.addAttribute("url5device", arrayList.get(12));
                model.addAttribute("url5gender", arrayList.get(13));
                model.addAttribute("url5age1", arrayList2);
                model.addAttribute("url5year3", arrayList.get(14));
                model.addAttribute("url5month3", arrayList.get(15));
                model.addAttribute("url5day3", arrayList.get(16));
                model.addAttribute("url5year4", arrayList.get(17));
                model.addAttribute("url5month4", arrayList.get(18));
                model.addAttribute("url5day4", arrayList.get(19));
                model.addAttribute("url5select_day_week_month2", arrayList.get(20));
                model.addAttribute("url5device2", arrayList.get(21));
                model.addAttribute("url5gender2", arrayList.get(22));
                model.addAttribute("url5age2", arrayList3);
                model.addAttribute("isTrue5", isTrue5);
            } else if (i==4&&favoriteURL==""){
                isTrue5 = false;
                model.addAttribute("isTrue5", isTrue5);
            }
        }

        return "favorite_url";
    }

    @PostMapping("/favorite_url")
    public String search_result(@RequestParam(name = "removeAllUrl", required = false, defaultValue = "") String removeAllUrl,
                                @RequestParam(name = "removeUrl1", required = false, defaultValue = "") String removeUrl1,
                                @RequestParam(name = "removeUrl2", required = false, defaultValue = "") String removeUrl2,
                                @RequestParam(name = "removeUrl3", required = false, defaultValue = "") String removeUrl3,
                                @RequestParam(name = "removeUrl4", required = false, defaultValue = "") String removeUrl4,
                                @RequestParam(name = "removeUrl5", required = false, defaultValue = "") String removeUrl5,
                                Model model,
                                Principal principal) {
        String userName = principal.getName();
        model.addAttribute("username", userName);

        String favoriteURL = "";

        model.addAttribute("username", userName);
        if (removeAllUrl.equals("1")){
            memberService.setRemoveAllUrl(userName);
        }
        if (removeUrl1.equals("1")) {
            memberService.setRemoveUrl1(userName);
        }
        if (removeUrl2.equals("2")) {
            memberService.setRemoveUrl2(userName);
        }
        if (removeUrl3.equals("3")) {
            memberService.setRemoveUrl3(userName);
        }
        if (removeUrl4.equals("4")) {
            memberService.setRemoveUrl4(userName);
        }
        if (removeUrl5.equals("5")) {
            memberService.setRemoveUrl5(userName);
        }

        boolean isTrue1 = true;
        boolean isTrue2 = true;
        boolean isTrue3 = true;
        boolean isTrue4 = true;
        boolean isTrue5 = true;

        for (int i = 0; i < memberService.getDbFavoriteURL(userName).size(); i++) {
            favoriteURL = memberService.getDbFavoriteURL(userName).get(i);
            ArrayList<String> arrayList = memberService.extractUrl(favoriteURL);
            ArrayList<String> arrayList2 = memberService.getAge1(favoriteURL);
            ArrayList<String> arrayList3 = memberService.getAge2(favoriteURL);

            if (arrayList2.size()==0){
                arrayList2.add("전체");
            }

            if (arrayList3.size()==0){
                arrayList3.add("전체");
            }

            if (favoriteURL!=""){
                for (int j = 0; j < arrayList2.size(); j++) {
                    if (arrayList2.get(j).equals("1")){
                        arrayList2.set(j,"~12");
                    } else if (arrayList2.get(j).equals("2")) {
                        arrayList2.set(j,"13~18");
                    } else if (arrayList2.get(j).equals("3")) {
                        arrayList2.set(j,"19~24");
                    } else if (arrayList2.get(j).equals("4")) {
                        arrayList2.set(j,"25~29");
                    } else if (arrayList2.get(j).equals("5")) {
                        arrayList2.set(j,"30~34");
                    } else if (arrayList2.get(j).equals("6")) {
                        arrayList2.set(j,"35~39");
                    } else if (arrayList2.get(j).equals("7")) {
                        arrayList2.set(j,"40~44");
                    } else if (arrayList2.get(j).equals("8")) {
                        arrayList2.set(j,"45~49");
                    } else if (arrayList2.get(j).equals("9")) {
                        arrayList2.set(j,"50~54");
                    } else if (arrayList2.get(j).equals("10")) {
                        arrayList2.set(j,"54~59");
                    } else if (arrayList2.get(j).equals("11")) {
                        arrayList2.set(j,"60~");
                    }
                }
                for (int j = 0; j < arrayList3.size(); j++) {
                    if (arrayList3.get(j).equals("1")){
                        arrayList3.set(j,"~12");
                    } else if (arrayList3.get(j).equals("2")) {
                        arrayList3.set(j,"13~18");
                    } else if (arrayList3.get(j).equals("3")) {
                        arrayList3.set(j,"19~24");
                    } else if (arrayList3.get(j).equals("4")) {
                        arrayList3.set(j,"25~29");
                    } else if (arrayList3.get(j).equals("5")) {
                        arrayList3.set(j,"30~34");
                    } else if (arrayList3.get(j).equals("6")) {
                        arrayList3.set(j,"35~39");
                    } else if (arrayList3.get(j).equals("7")) {
                        arrayList3.set(j,"40~44");
                    } else if (arrayList3.get(j).equals("8")) {
                        arrayList3.set(j,"45~49");
                    } else if (arrayList3.get(j).equals("9")) {
                        arrayList3.set(j,"50~54");
                    } else if (arrayList3.get(j).equals("10")) {
                        arrayList3.set(j,"54~59");
                    } else if (arrayList3.get(j).equals("11")) {
                        arrayList3.set(j,"60~");
                    }
                }
            }

            if (i == 0&&favoriteURL!="") {
                model.addAttribute("url1", favoriteURL);
                model.addAttribute("url1query1", arrayList.get(0));
                model.addAttribute("url1query2", arrayList.get(1));
                model.addAttribute("url1query3", arrayList.get(2));
                model.addAttribute("url1query4", arrayList.get(3));
                model.addAttribute("url1query5", arrayList.get(4));
                model.addAttribute("url1year1", arrayList.get(5));
                model.addAttribute("url1month1", arrayList.get(6));
                model.addAttribute("url1day1", arrayList.get(7));
                model.addAttribute("url1year2", arrayList.get(8));
                model.addAttribute("url1month2", arrayList.get(9));
                model.addAttribute("url1day2", arrayList.get(10));
                model.addAttribute("url1select_day_week_month", arrayList.get(11));
                model.addAttribute("url1device", arrayList.get(12));
                model.addAttribute("url1gender", arrayList.get(13));
                model.addAttribute("url1age1", arrayList2);
                model.addAttribute("url1year3", arrayList.get(14));
                model.addAttribute("url1month3", arrayList.get(15));
                model.addAttribute("url1day3", arrayList.get(16));
                model.addAttribute("url1year4", arrayList.get(17));
                model.addAttribute("url1month4", arrayList.get(18));
                model.addAttribute("url1day4", arrayList.get(19));
                model.addAttribute("url1select_day_week_month2", arrayList.get(20));
                model.addAttribute("url1device2", arrayList.get(21));
                model.addAttribute("url1gender2", arrayList.get(22));
                model.addAttribute("url1age2", arrayList3);
                model.addAttribute("isTrue1", isTrue1);
            } else if (i==0&&favoriteURL==""){
                isTrue1 = false;
                model.addAttribute("isTrue1", isTrue1);
            }
            if (i == 1&&favoriteURL!="") {
                model.addAttribute("url2", favoriteURL);
                model.addAttribute("url2query1", arrayList.get(0));
                model.addAttribute("url2query2", arrayList.get(1));
                model.addAttribute("url2query3", arrayList.get(2));
                model.addAttribute("url2query4", arrayList.get(3));
                model.addAttribute("url2query5", arrayList.get(4));
                model.addAttribute("url2year1", arrayList.get(5));
                model.addAttribute("url2month1", arrayList.get(6));
                model.addAttribute("url2day1", arrayList.get(7));
                model.addAttribute("url2year2", arrayList.get(8));
                model.addAttribute("url2month2", arrayList.get(9));
                model.addAttribute("url2day2", arrayList.get(10));
                model.addAttribute("url2select_day_week_month", arrayList.get(11));
                model.addAttribute("url2device", arrayList.get(12));
                model.addAttribute("url2gender", arrayList.get(13));
                model.addAttribute("url2age1", arrayList2);
                model.addAttribute("url2year3", arrayList.get(14));
                model.addAttribute("url2month3", arrayList.get(15));
                model.addAttribute("url2day3", arrayList.get(16));
                model.addAttribute("url2year4", arrayList.get(17));
                model.addAttribute("url2month4", arrayList.get(18));
                model.addAttribute("url2day4", arrayList.get(19));
                model.addAttribute("url2select_day_week_month2", arrayList.get(20));
                model.addAttribute("url2device2", arrayList.get(21));
                model.addAttribute("url2gender2", arrayList.get(22));
                model.addAttribute("url2age2", arrayList3);
                model.addAttribute("isTrue2", isTrue2);
            } else if (i==1&&favoriteURL==""){
                isTrue2 = false;
                model.addAttribute("isTrue2", isTrue2);
            }
            if (i == 2&&favoriteURL!="") {
                model.addAttribute("url3", favoriteURL);
                model.addAttribute("url3query1", arrayList.get(0));
                model.addAttribute("url3query2", arrayList.get(1));
                model.addAttribute("url3query3", arrayList.get(2));
                model.addAttribute("url3query4", arrayList.get(3));
                model.addAttribute("url3query5", arrayList.get(4));
                model.addAttribute("url3year1", arrayList.get(5));
                model.addAttribute("url3month1", arrayList.get(6));
                model.addAttribute("url3day1", arrayList.get(7));
                model.addAttribute("url3year2", arrayList.get(8));
                model.addAttribute("url3month2", arrayList.get(9));
                model.addAttribute("url3day2", arrayList.get(10));
                model.addAttribute("url3select_day_week_month", arrayList.get(11));
                model.addAttribute("url3device", arrayList.get(12));
                model.addAttribute("url3gender", arrayList.get(13));
                model.addAttribute("url3age1", arrayList2);
                model.addAttribute("url3year3", arrayList.get(14));
                model.addAttribute("url3month3", arrayList.get(15));
                model.addAttribute("url3day3", arrayList.get(16));
                model.addAttribute("url3year4", arrayList.get(17));
                model.addAttribute("url3month4", arrayList.get(18));
                model.addAttribute("url3day4", arrayList.get(19));
                model.addAttribute("url3select_day_week_month2", arrayList.get(20));
                model.addAttribute("url3device2", arrayList.get(21));
                model.addAttribute("url3gender2", arrayList.get(22));
                model.addAttribute("url3age2", arrayList3);
                model.addAttribute("isTrue3", isTrue3);
            } else if (i==2&&favoriteURL==""){
                isTrue3 = false;
                model.addAttribute("isTrue3", isTrue3);
            }
            if (i == 3&&favoriteURL!="") {
                model.addAttribute("url4", favoriteURL);
                model.addAttribute("url4query1", arrayList.get(0));
                model.addAttribute("url4query2", arrayList.get(1));
                model.addAttribute("url4query3", arrayList.get(2));
                model.addAttribute("url4query4", arrayList.get(3));
                model.addAttribute("url4query5", arrayList.get(4));
                model.addAttribute("url4year1", arrayList.get(5));
                model.addAttribute("url4month1", arrayList.get(6));
                model.addAttribute("url4day1", arrayList.get(7));
                model.addAttribute("url4year2", arrayList.get(8));
                model.addAttribute("url4month2", arrayList.get(9));
                model.addAttribute("url4day2", arrayList.get(10));
                model.addAttribute("url4select_day_week_month", arrayList.get(11));
                model.addAttribute("url4device", arrayList.get(12));
                model.addAttribute("url4gender", arrayList.get(13));
                model.addAttribute("url4age1", arrayList2);
                model.addAttribute("url4year3", arrayList.get(14));
                model.addAttribute("url4month3", arrayList.get(15));
                model.addAttribute("url4day3", arrayList.get(16));
                model.addAttribute("url4year4", arrayList.get(17));
                model.addAttribute("url4month4", arrayList.get(18));
                model.addAttribute("url4day4", arrayList.get(19));
                model.addAttribute("url4select_day_week_month2", arrayList.get(20));
                model.addAttribute("url4device2", arrayList.get(21));
                model.addAttribute("url4gender2", arrayList.get(22));
                model.addAttribute("url4age2", arrayList3);
                model.addAttribute("isTrue4", isTrue4);
            } else if (i==3&&favoriteURL==""){
                isTrue4 = false;
                model.addAttribute("isTrue4", isTrue4);
            }
            if (i == 4&&favoriteURL!="") {
                model.addAttribute("url5", favoriteURL);
                model.addAttribute("url5query1", arrayList.get(0));
                model.addAttribute("url5query2", arrayList.get(1));
                model.addAttribute("url5query3", arrayList.get(2));
                model.addAttribute("url5query4", arrayList.get(3));
                model.addAttribute("url5query5", arrayList.get(4));
                model.addAttribute("url5year1", arrayList.get(5));
                model.addAttribute("url5month1", arrayList.get(6));
                model.addAttribute("url5day1", arrayList.get(7));
                model.addAttribute("url5year2", arrayList.get(8));
                model.addAttribute("url5month2", arrayList.get(9));
                model.addAttribute("url5day2", arrayList.get(10));
                model.addAttribute("url5select_day_week_month", arrayList.get(11));
                model.addAttribute("url5device", arrayList.get(12));
                model.addAttribute("url5gender", arrayList.get(13));
                model.addAttribute("url5age1", arrayList2);
                model.addAttribute("url5year3", arrayList.get(14));
                model.addAttribute("url5month3", arrayList.get(15));
                model.addAttribute("url5day3", arrayList.get(16));
                model.addAttribute("url5year4", arrayList.get(17));
                model.addAttribute("url5month4", arrayList.get(18));
                model.addAttribute("url5day4", arrayList.get(19));
                model.addAttribute("url5select_day_week_month2", arrayList.get(20));
                model.addAttribute("url5device2", arrayList.get(21));
                model.addAttribute("url5gender2", arrayList.get(22));
                model.addAttribute("url5age2", arrayList3);
                model.addAttribute("isTrue5", isTrue5);
            } else if (i==4&&favoriteURL==""){
                isTrue5 = false;
                model.addAttribute("isTrue5", isTrue5);
            }
        }

        return "favorite_url";
    }

    @GetMapping("/calendarFavoriteUrl")
    public String calendarFavoriteUrl( Model model,
                                       @RequestParam(name = "url", required = false, defaultValue = "") String url,
                                       Principal principal) {
        String userName = principal.getName();
        model.addAttribute("username", userName);
        String favoriteURL = "";

        if (!url.equals(memberService.getDbCalFavoriteURL(userName).get(0))&&!url.equals(memberService.getDbCalFavoriteURL(userName).get(1))&&!url.equals(memberService.getDbCalFavoriteURL(userName).get(2))&&!url.equals(memberService.getDbCalFavoriteURL(userName).get(3))&&!url.equals(memberService.getDbCalFavoriteURL(userName).get(4))){
            memberService.setDbCalFavoriteURL(url,userName);
        }

        boolean isTrue1 = true;
        boolean isTrue2 = true;
        boolean isTrue3 = true;
        boolean isTrue4 = true;
        boolean isTrue5 = true;

        for (int i = 0; i < memberService.getDbCalFavoriteURL(userName).size(); i++) {
            favoriteURL = memberService.getDbCalFavoriteURL(userName).get(i);
            ArrayList<String> arrayList = memberService.extractCalUrl(favoriteURL);
            ArrayList<String> arrayList2 = memberService.getAge(favoriteURL);

            if (arrayList2.size()==0){
                arrayList2.add("전체");
            }

            if (favoriteURL!=""){
                for (int j = 0; j < arrayList2.size(); j++) {
                    if (arrayList2.get(j).equals("1")){
                        arrayList2.set(j,"~12");
                    } else if (arrayList2.get(j).equals("2")) {
                        arrayList2.set(j,"13~18");
                    } else if (arrayList2.get(j).equals("3")) {
                        arrayList2.set(j,"19~24");
                    } else if (arrayList2.get(j).equals("4")) {
                        arrayList2.set(j,"25~29");
                    } else if (arrayList2.get(j).equals("5")) {
                        arrayList2.set(j,"30~34");
                    } else if (arrayList2.get(j).equals("6")) {
                        arrayList2.set(j,"35~39");
                    } else if (arrayList2.get(j).equals("7")) {
                        arrayList2.set(j,"40~44");
                    } else if (arrayList2.get(j).equals("8")) {
                        arrayList2.set(j,"45~49");
                    } else if (arrayList2.get(j).equals("9")) {
                        arrayList2.set(j,"50~54");
                    } else if (arrayList2.get(j).equals("10")) {
                        arrayList2.set(j,"54~59");
                    } else if (arrayList2.get(j).equals("11")) {
                        arrayList2.set(j,"60~");
                    }
                }
            }

            if (i == 0&&favoriteURL!="") {
                model.addAttribute("url1", favoriteURL);
                model.addAttribute("url1query1", arrayList.get(0));
                model.addAttribute("url1query2", arrayList.get(1));
                model.addAttribute("url1query3", arrayList.get(2));
                model.addAttribute("url1query4", arrayList.get(3));
                model.addAttribute("url1query5", arrayList.get(4));
                model.addAttribute("url1year1", arrayList.get(5));
                model.addAttribute("url1month1", arrayList.get(6));
                model.addAttribute("url1day1", arrayList.get(7));
                model.addAttribute("url1year2", arrayList.get(8));
                model.addAttribute("url1month2", arrayList.get(9));
                model.addAttribute("url1day2", arrayList.get(10));
                model.addAttribute("url1select_day_week_month", arrayList.get(11));
                model.addAttribute("url1device", arrayList.get(12));
                model.addAttribute("url1gender", arrayList.get(13));
                model.addAttribute("url1age1", arrayList2);
                model.addAttribute("isTrue1", isTrue1);
            } else if (i==0&&favoriteURL==""){
                isTrue1 = false;
                model.addAttribute("isTrue1", isTrue1);
            }
            if (i == 1&&favoriteURL!="") {
                model.addAttribute("url2", favoriteURL);
                model.addAttribute("url2query1", arrayList.get(0));
                model.addAttribute("url2query2", arrayList.get(1));
                model.addAttribute("url2query3", arrayList.get(2));
                model.addAttribute("url2query4", arrayList.get(3));
                model.addAttribute("url2query5", arrayList.get(4));
                model.addAttribute("url2year1", arrayList.get(5));
                model.addAttribute("url2month1", arrayList.get(6));
                model.addAttribute("url2day1", arrayList.get(7));
                model.addAttribute("url2year2", arrayList.get(8));
                model.addAttribute("url2month2", arrayList.get(9));
                model.addAttribute("url2day2", arrayList.get(10));
                model.addAttribute("url2select_day_week_month", arrayList.get(11));
                model.addAttribute("url2device", arrayList.get(12));
                model.addAttribute("url2gender", arrayList.get(13));
                model.addAttribute("url2age1", arrayList2);
                model.addAttribute("isTrue2", isTrue2);
            } else if (i==1&&favoriteURL==""){
                isTrue2 = false;
                model.addAttribute("isTrue2", isTrue2);
            }
            if (i == 2&&favoriteURL!="") {
                model.addAttribute("url3", favoriteURL);
                model.addAttribute("url3query1", arrayList.get(0));
                model.addAttribute("url3query2", arrayList.get(1));
                model.addAttribute("url3query3", arrayList.get(2));
                model.addAttribute("url3query4", arrayList.get(3));
                model.addAttribute("url3query5", arrayList.get(4));
                model.addAttribute("url3year1", arrayList.get(5));
                model.addAttribute("url3month1", arrayList.get(6));
                model.addAttribute("url3day1", arrayList.get(7));
                model.addAttribute("url3year2", arrayList.get(8));
                model.addAttribute("url3month2", arrayList.get(9));
                model.addAttribute("url3day2", arrayList.get(10));
                model.addAttribute("url3select_day_week_month", arrayList.get(11));
                model.addAttribute("url3device", arrayList.get(12));
                model.addAttribute("url3gender", arrayList.get(13));
                model.addAttribute("url3age1", arrayList2);
                model.addAttribute("isTrue3", isTrue3);
            } else if (i==2&&favoriteURL==""){
                isTrue3 = false;
                model.addAttribute("isTrue3", isTrue3);
            }
            if (i == 3&&favoriteURL!="") {
                model.addAttribute("url4", favoriteURL);
                model.addAttribute("url4query1", arrayList.get(0));
                model.addAttribute("url4query2", arrayList.get(1));
                model.addAttribute("url4query3", arrayList.get(2));
                model.addAttribute("url4query4", arrayList.get(3));
                model.addAttribute("url4query5", arrayList.get(4));
                model.addAttribute("url4year1", arrayList.get(5));
                model.addAttribute("url4month1", arrayList.get(6));
                model.addAttribute("url4day1", arrayList.get(7));
                model.addAttribute("url4year2", arrayList.get(8));
                model.addAttribute("url4month2", arrayList.get(9));
                model.addAttribute("url4day2", arrayList.get(10));
                model.addAttribute("url4select_day_week_month", arrayList.get(11));
                model.addAttribute("url4device", arrayList.get(12));
                model.addAttribute("url4gender", arrayList.get(13));
                model.addAttribute("url4age1", arrayList2);
                model.addAttribute("isTrue4", isTrue4);
            } else if (i==3&&favoriteURL==""){
                isTrue4 = false;
                model.addAttribute("isTrue4", isTrue4);
            }
            if (i == 4&&favoriteURL!="") {
                model.addAttribute("url5", favoriteURL);
                model.addAttribute("url5query1", arrayList.get(0));
                model.addAttribute("url5query2", arrayList.get(1));
                model.addAttribute("url5query3", arrayList.get(2));
                model.addAttribute("url5query4", arrayList.get(3));
                model.addAttribute("url5query5", arrayList.get(4));
                model.addAttribute("url5year1", arrayList.get(5));
                model.addAttribute("url5month1", arrayList.get(6));
                model.addAttribute("url5day1", arrayList.get(7));
                model.addAttribute("url5year2", arrayList.get(8));
                model.addAttribute("url5month2", arrayList.get(9));
                model.addAttribute("url5day2", arrayList.get(10));
                model.addAttribute("url5select_day_week_month", arrayList.get(11));
                model.addAttribute("url5device", arrayList.get(12));
                model.addAttribute("url5gender", arrayList.get(13));
                model.addAttribute("url5age1", arrayList2);
                model.addAttribute("isTrue5", isTrue5);
            } else if (i==4&&favoriteURL==""){
                isTrue5 = false;
                model.addAttribute("isTrue5", isTrue5);
            }
        }

        return "calendarFavoriteUrl";
    }

    @PostMapping("/calendarFavoriteUrl")
    public String calendarFavoriteUrl(@RequestParam(name = "removeAllUrl", required = false, defaultValue = "") String removeAllUrl,
                                      @RequestParam(name = "removeUrl1", required = false, defaultValue = "") String removeUrl1,
                                      @RequestParam(name = "removeUrl2", required = false, defaultValue = "") String removeUrl2,
                                      @RequestParam(name = "removeUrl3", required = false, defaultValue = "") String removeUrl3,
                                      @RequestParam(name = "removeUrl4", required = false, defaultValue = "") String removeUrl4,
                                      @RequestParam(name = "removeUrl5", required = false, defaultValue = "") String removeUrl5,
                                      Model model,
                                      Principal principal) throws JSONException {
        String userName = principal.getName();
        model.addAttribute("username", userName);

        String favoriteURL = "";

        if (removeAllUrl.equals("1")){
            memberService.setRemoveAllCalUrl(userName);
        }
        if (removeUrl1.equals("1")) {
            memberService.setRemoveCalUrl1(userName);
        }
        if (removeUrl2.equals("2")) {
            memberService.setRemoveCalUrl2(userName);
        }
        if (removeUrl3.equals("3")) {
            memberService.setRemoveCalUrl3(userName);
        }
        if (removeUrl4.equals("4")) {
            memberService.setRemoveCalUrl4(userName);
        }
        if (removeUrl5.equals("5")) {
            memberService.setRemoveCalUrl5(userName);
        }

        boolean isTrue1 = true;
        boolean isTrue2 = true;
        boolean isTrue3 = true;
        boolean isTrue4 = true;
        boolean isTrue5 = true;

        for (int i = 0; i < memberService.getDbCalFavoriteURL(userName).size(); i++) {
            favoriteURL = memberService.getDbCalFavoriteURL(userName).get(i);
            ArrayList<String> arrayList = memberService.extractCalUrl(favoriteURL);
            ArrayList<String> arrayList2 = memberService.getAge(favoriteURL);

            if (arrayList2.size()==0){
                arrayList2.add("전체");
            }

            if (favoriteURL!=""){
                for (int j = 0; j < arrayList2.size(); j++) {
                    if (arrayList2.get(j).equals("1")){
                        arrayList2.set(j,"~12");
                    } else if (arrayList2.get(j).equals("2")) {
                        arrayList2.set(j,"13~18");
                    } else if (arrayList2.get(j).equals("3")) {
                        arrayList2.set(j,"19~24");
                    } else if (arrayList2.get(j).equals("4")) {
                        arrayList2.set(j,"25~29");
                    } else if (arrayList2.get(j).equals("5")) {
                        arrayList2.set(j,"30~34");
                    } else if (arrayList2.get(j).equals("6")) {
                        arrayList2.set(j,"35~39");
                    } else if (arrayList2.get(j).equals("7")) {
                        arrayList2.set(j,"40~44");
                    } else if (arrayList2.get(j).equals("8")) {
                        arrayList2.set(j,"45~49");
                    } else if (arrayList2.get(j).equals("9")) {
                        arrayList2.set(j,"50~54");
                    } else if (arrayList2.get(j).equals("10")) {
                        arrayList2.set(j,"54~59");
                    } else if (arrayList2.get(j).equals("11")) {
                        arrayList2.set(j,"60~");
                    }
                }
            }

            if (i == 0&&favoriteURL!="") {
                model.addAttribute("url1", favoriteURL);
                model.addAttribute("url1query1", arrayList.get(0));
                model.addAttribute("url1query2", arrayList.get(1));
                model.addAttribute("url1query3", arrayList.get(2));
                model.addAttribute("url1query4", arrayList.get(3));
                model.addAttribute("url1query5", arrayList.get(4));
                model.addAttribute("url1year1", arrayList.get(5));
                model.addAttribute("url1month1", arrayList.get(6));
                model.addAttribute("url1day1", arrayList.get(7));
                model.addAttribute("url1year2", arrayList.get(8));
                model.addAttribute("url1month2", arrayList.get(9));
                model.addAttribute("url1day2", arrayList.get(10));
                model.addAttribute("url1select_day_week_month", arrayList.get(11));
                model.addAttribute("url1device", arrayList.get(12));
                model.addAttribute("url1gender", arrayList.get(13));
                model.addAttribute("url1age1", arrayList2);
                model.addAttribute("isTrue1", isTrue1);
            } else if (i==0&&favoriteURL==""){
                isTrue1 = false;
                model.addAttribute("isTrue1", isTrue1);
            }
            if (i == 1&&favoriteURL!="") {
                model.addAttribute("url2", favoriteURL);
                model.addAttribute("url2query1", arrayList.get(0));
                model.addAttribute("url2query2", arrayList.get(1));
                model.addAttribute("url2query3", arrayList.get(2));
                model.addAttribute("url2query4", arrayList.get(3));
                model.addAttribute("url2query5", arrayList.get(4));
                model.addAttribute("url2year1", arrayList.get(5));
                model.addAttribute("url2month1", arrayList.get(6));
                model.addAttribute("url2day1", arrayList.get(7));
                model.addAttribute("url2year2", arrayList.get(8));
                model.addAttribute("url2month2", arrayList.get(9));
                model.addAttribute("url2day2", arrayList.get(10));
                model.addAttribute("url2select_day_week_month", arrayList.get(11));
                model.addAttribute("url2device", arrayList.get(12));
                model.addAttribute("url2gender", arrayList.get(13));
                model.addAttribute("url2age1", arrayList2);
                model.addAttribute("isTrue2", isTrue2);
            } else if (i==1&&favoriteURL==""){
                isTrue2 = false;
                model.addAttribute("isTrue2", isTrue2);
            }
            if (i == 2&&favoriteURL!="") {
                model.addAttribute("url3", favoriteURL);
                model.addAttribute("url3query1", arrayList.get(0));
                model.addAttribute("url3query2", arrayList.get(1));
                model.addAttribute("url3query3", arrayList.get(2));
                model.addAttribute("url3query4", arrayList.get(3));
                model.addAttribute("url3query5", arrayList.get(4));
                model.addAttribute("url3year1", arrayList.get(5));
                model.addAttribute("url3month1", arrayList.get(6));
                model.addAttribute("url3day1", arrayList.get(7));
                model.addAttribute("url3year2", arrayList.get(8));
                model.addAttribute("url3month2", arrayList.get(9));
                model.addAttribute("url3day2", arrayList.get(10));
                model.addAttribute("url3select_day_week_month", arrayList.get(11));
                model.addAttribute("url3device", arrayList.get(12));
                model.addAttribute("url3gender", arrayList.get(13));
                model.addAttribute("url3age1", arrayList2);
                model.addAttribute("isTrue3", isTrue3);
            } else if (i==2&&favoriteURL==""){
                isTrue3 = false;
                model.addAttribute("isTrue3", isTrue3);
            }
            if (i == 3&&favoriteURL!="") {
                model.addAttribute("url4", favoriteURL);
                model.addAttribute("url4query1", arrayList.get(0));
                model.addAttribute("url4query2", arrayList.get(1));
                model.addAttribute("url4query3", arrayList.get(2));
                model.addAttribute("url4query4", arrayList.get(3));
                model.addAttribute("url4query5", arrayList.get(4));
                model.addAttribute("url4year1", arrayList.get(5));
                model.addAttribute("url4month1", arrayList.get(6));
                model.addAttribute("url4day1", arrayList.get(7));
                model.addAttribute("url4year2", arrayList.get(8));
                model.addAttribute("url4month2", arrayList.get(9));
                model.addAttribute("url4day2", arrayList.get(10));
                model.addAttribute("url4select_day_week_month", arrayList.get(11));
                model.addAttribute("url4device", arrayList.get(12));
                model.addAttribute("url4gender", arrayList.get(13));
                model.addAttribute("url4age1", arrayList2);
                model.addAttribute("isTrue4", isTrue4);
            } else if (i==3&&favoriteURL==""){
                isTrue4 = false;
                model.addAttribute("isTrue4", isTrue4);
            }
            if (i == 4&&favoriteURL!="") {
                model.addAttribute("url5", favoriteURL);
                model.addAttribute("url5query1", arrayList.get(0));
                model.addAttribute("url5query2", arrayList.get(1));
                model.addAttribute("url5query3", arrayList.get(2));
                model.addAttribute("url5query4", arrayList.get(3));
                model.addAttribute("url5query5", arrayList.get(4));
                model.addAttribute("url5year1", arrayList.get(5));
                model.addAttribute("url5month1", arrayList.get(6));
                model.addAttribute("url5day1", arrayList.get(7));
                model.addAttribute("url5year2", arrayList.get(8));
                model.addAttribute("url5month2", arrayList.get(9));
                model.addAttribute("url5day2", arrayList.get(10));
                model.addAttribute("url5select_day_week_month", arrayList.get(11));
                model.addAttribute("url5device", arrayList.get(12));
                model.addAttribute("url5gender", arrayList.get(13));
                model.addAttribute("url5age1", arrayList2);
                model.addAttribute("isTrue5", isTrue5);
            } else if (i==4&&favoriteURL==""){
                isTrue5 = false;
                model.addAttribute("isTrue5", isTrue5);
            }
        }

        return "calendarFavoriteUrl";
    }

    @GetMapping("/index_result")
    public String search(Principal principal,
                         @RequestParam(name = "query1", required = false, defaultValue = "") String query1,
                         @RequestParam(name = "query2", required = false, defaultValue = "") String query2,
                         @RequestParam(name = "query3", required = false, defaultValue = "") String query3,
                         @RequestParam(name = "query4", required = false, defaultValue = "") String query4,
                         @RequestParam(name = "query5", required = false, defaultValue = "") String query5,
                         @RequestParam(name = "year", required = false, defaultValue = "") String year1,
                         @RequestParam(name = "month", required = false, defaultValue = "") String month1,
                         @RequestParam(name = "day", required = false, defaultValue = "") String day1,
                         @RequestParam(name = "year2", required = false, defaultValue = "") String year2,
                         @RequestParam(name = "month2", required = false, defaultValue = "") String month2,
                         @RequestParam(name = "day2", required = false, defaultValue = "") String day2,
                         @RequestParam(name = "select_day_week_month", required = false, defaultValue = "") String timeunit,
                         @RequestParam(name = "device", required = false, defaultValue = "") String coverage,
                         @RequestParam(name = "gender", required = false, defaultValue = "") String gender,
                         @RequestParam(name = "age", required = false, defaultValue = "") String[] age,
                         @RequestParam(name = "year3", required = false, defaultValue = "") String year3,
                         @RequestParam(name = "month3", required = false, defaultValue = "") String month3,
                         @RequestParam(name = "day3", required = false, defaultValue = "") String day3,
                         @RequestParam(name = "year4", required = false, defaultValue = "") String year4,
                         @RequestParam(name = "month4", required = false, defaultValue = "") String month4,
                         @RequestParam(name = "day4", required = false, defaultValue = "") String day4,
                         @RequestParam(name = "select_day_week_month2", required = false, defaultValue = "") String timeunit2,
                         @RequestParam(name = "device2", required = false, defaultValue = "") String coverage2,
                         @RequestParam(name = "gender2", required = false, defaultValue = "") String gender2,
                         @RequestParam(name = "age2", required = false, defaultValue = "") String[] age2,
                         Model model) throws JSONException {
        String userName = principal.getName();

        if (!query1.equals("")){
            String jsonString1 = memberService.NaverApiResponse(query1, query2, query3, query4, query5, year1, month1, day1, year2, month2, day2, timeunit, coverage, gender, age);
            String jsonString2 = memberService.NaverApiResponse(query1, query2, query3, query4, query5, year3, month3, day3, year4, month4, day4, timeunit2, coverage2, gender2, age2);

            ResultMaps result1Maps = memberService.parseJson(jsonString1);
            ResultMaps result2Maps = memberService.parseJson(jsonString2);

            int i = result1Maps.getPeriodsMap().size();
            for (String title : result1Maps.getPeriodsMap().keySet()) {
                model.addAttribute("query"+i, title);
                if (i==result1Maps.getPeriodsMap().size()){
                    model.addAttribute("xAxisData", result1Maps.getPeriodsMap().get(title));
                }
                model.addAttribute("seriesData"+i, result1Maps.getRatiosMap().get(title));
                System.out.println(title);
                i--;
            }
            int j = result2Maps.getPeriodsMap().size();
            for (String title : result2Maps.getPeriodsMap().keySet()) {
                if (j==result2Maps.getPeriodsMap().size()){
                    model.addAttribute("xAxis2Data", result2Maps.getPeriodsMap().get(title));
                }
                model.addAttribute("series2Data"+j, result2Maps.getRatiosMap().get(title));
                j--;
            }
        }

        model.addAttribute("username", userName);

        String favoriteURL = "";

        for (int i = 0; i < memberService.getDbFavoriteURL(userName).size(); i++) {
            favoriteURL = memberService.getDbFavoriteURL(userName).get(i);
            model.addAttribute("url"+(i+1), favoriteURL);
        }

        boolean isTrue1 = true;
        boolean isTrue2 = true;
        boolean isTrue3 = true;
        boolean isTrue4 = true;
        boolean isTrue5 = true;
        boolean isTrueAll = true;

        for (int i = 0; i < memberService.getDbFavoriteURL(userName).size(); i++) {
            favoriteURL = memberService.getDbFavoriteURL(userName).get(i);

            if (i==0&&favoriteURL==""){
                isTrue1 = false;
            }
            if (i==1&&favoriteURL==""){
                isTrue2 = false;
            }
            if (i==2&&favoriteURL==""){
                isTrue3 = false;
            }
            if (i==3&&favoriteURL==""){
                isTrue4 = false;
            }
            if (i==4&&favoriteURL==""){
                isTrue5 = false;
            }
            if (isTrue1==true&&isTrue2==true&&isTrue3==true&&isTrue4==true&&isTrue5==true){
                model.addAttribute("isTrueAll", isTrueAll);
            } else {
                isTrueAll = false;
                model.addAttribute("isTrueAll", isTrueAll);
            }
        }

        return "index_result";
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/index_login_success")
    public String home_success(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("username", username);
        return "index_login_success";
    }
    @GetMapping("/about_success")
    public String home_about_success(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("username", username);
        return "about_success";
    }

    @GetMapping("/service_success")
    public String home_service_success(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("username", username);
        return "Analysis_success";
    }

    @GetMapping("/team_success")
    public String home_team_success(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("username", username);
        return "team_success";
    }

    @GetMapping("/manual_success")
    public String home_why_success(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("username", username);
        return "manual_success";
    }

    @GetMapping("/about")
    public String home_about(Principal principal, Model model) {

        return "about";
    }

    @GetMapping("/team")
    public String home_team(Principal principal, Model model) {

        return "team";
    }

    @GetMapping("/service")
    public String home_service(Principal principal, Model model) {

        return "Analysis";
    }

    @GetMapping("/manual")
    public String home_why(Principal principal, Model model) {

        return "manual";
    }

    @GetMapping("/member/reset/password")
    public String resetPassword(Model model, HttpServletRequest request) {
        String uuid = request.getParameter("id");

        boolean result = memberService.checkResetPassword(uuid);

        model.addAttribute("result", result);

        return "member/reset_password";
    }

    @PostMapping("/member/reset/password")
    public String resetPasswordSubmit(Model model, ResetPasswordInput parameter) {
        boolean result = false;
        try{
            result = memberService.resetPassword(parameter.getId(), parameter.getPassword());
        } catch (Exception e) {
        }

        model.addAttribute("result", result);

        return "member/reset_password_result";
    }

    @GetMapping("/member/info")
    public String memberInfo() {

        return "member/info";
    }

    @GetMapping("/changePwd")
    public String changePwd(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("username", username);
        return "changePwd";
    }

    @PostMapping("/changePwd")
    public String changePwd(Model model, ResetPasswordInput parameter) {

        boolean result = false;
        try {
            result = memberService.sendResetPassword(parameter);
        }catch (Exception e) {
        }

        model.addAttribute("result", result);

        return "member/change_password_result";
    }

    @GetMapping("/member/find/password")
    public String findPassword() {

        return "member/find_password";
    }

    @PostMapping("/member/find/password")
    public String findPasswordSubmit(Model model, ResetPasswordInput parameter) {

        boolean result = false;
        try {
            result = memberService.sendResetPassword(parameter);
        }catch (Exception e) {
        }

        model.addAttribute("result", result);

        return "member/find_password_result";
    }



}