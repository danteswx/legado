package io.legado.app.help.source

import io.legado.app.data.entities.BookSource

object UaaLoginCompat {

    const val BOOK_SOURCE_URL = "https://www.uaa.com"

    val LOGIN_UI: String = """
        <js>
        JSON.stringify([
        {name:"账号",type:"text"},
        {name:"密码",type:"password"},
        {name:"网页登录兜底",type:"button",action:"openWebLogin()",style:{layout_flexGrow:1,layout_flexBasisPercent:1}}
        ]);
        </js>
    """.trimIndent()

    val LOGIN_URL: String = """
        <js>
        var uaaBase="https://www.uaa.com";
        function gv(m,k){try{return String(m.get(k)||"").trim()}catch(e){return ""}}
        function page(){var h=String(java.ajax(uaaBase)||"");source.put("uaaLoginHtml",h);return h}
        function cap(h){var d=org.jsoup.Jsoup.parse(h,uaaBase);var i=d.select("img[src*=captcha],img[src*=verify],img[src*=code],img[alt*=验证码],img[id*=captcha],img[class*=captcha],img[id*=verify],img[class*=verify]").first();var u=i?i.absUrl("src"):"";source.put("uaaCaptchaUrl",u);return u}
        function solveUaaCaptcha(h){var u=cap(h||page());if(!u)throw "登录失败：未找到UAA验证码";var c=String(java.aiCaptcha(u,"计算这张UAA验证码里的三位个位数加减乘公式，只返回最终整数结果，不要解释，不要返回公式")).trim();if(!/^-?\d+$/.test(c))throw "登录失败：AI验证码结果无效："+c;source.put("uaaCaptchaAnswer",c);return c}
        function openWebLogin(){java.showBrowser(uaaBase,null,null,null)}
        function ck(cs){var a=[];try{var it=cs.entrySet().iterator();while(it.hasNext()){var e=it.next();a.push(e.getKey()+"="+e.getValue())}}catch(e){}return a.join("; ")}
        function login(){var info=typeof result!="undefined"?result:source.getLoginInfoMap(),u=gv(info,"账号"),p=gv(info,"密码");if(!u||!p)throw "登录失败：请输入账号和密码";var h=String(source.get("uaaLoginHtml")||"")||page(),c=solveUaaCaptcha(h),d=org.jsoup.Jsoup.parse(h,uaaBase),f=d.select("form:has(input[type=password]),form[action*=login],form").first(),a=f?f.absUrl("action"):"";if(!a)a=uaaBase+"/login";var data={username:u,password:p,captcha:c,verifyCode:c,code:c};if(f){var ins=f.select("input[name]");for(var i=0;i<ins.size();i++){var n=String(ins.get(i).attr("name")),l=n.toLowerCase(),v=String(ins.get(i).attr("value")||"");if(/pass|pwd/.test(l))v=p;else if(/user|account|email|phone|name/.test(l))v=u;else if(/captcha|verify|code/.test(l))v=c;data[n]=v}}var arr=[];for(var k in data){if(data.hasOwnProperty(k))arr.push(java.encodeURI(k)+"="+java.encodeURI(String(data[k])))}var r=java.post(a,arr.join("&"),{"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","Referer":uaaBase,"User-Agent":java.getWebViewUA()});var ch=ck(r.cookies());if(ch)source.putLoginHeader(JSON.stringify({Cookie:ch}));var b=String(r.body()||"");if(!ch&&/验证码|密码|错误|失败|invalid|incorrect/i.test(b))throw "登录失败："+b.replace(/<[^>]+>/g," ").replace(/\s+/g," ").substring(0,80);java.toast("UAA登录成功")}
        </js>
    """.trimIndent()

    fun isUaaSource(sourceUrl: String?): Boolean {
        return sourceUrl
            ?.trim()
            ?.trimEnd('/')
            ?.equals(BOOK_SOURCE_URL, ignoreCase = true) == true
    }

    fun apply(source: BookSource): Boolean {
        if (!isUaaSource(source.bookSourceUrl)) {
            return false
        }

        var changed = false
        if (shouldReplaceLoginUi(source.loginUi)) {
            source.loginUi = LOGIN_UI
            changed = true
        }
        if (shouldReplaceLoginUrl(source.loginUrl)) {
            source.loginUrl = LOGIN_URL
            changed = true
        }
        return changed
    }

    private fun shouldReplaceLoginUi(loginUi: String?): Boolean {
        val text = loginUi?.trim().orEmpty()
        if (text.isBlank()) {
            return true
        }
        return text.contains("AI识别验证码") ||
            text.contains("refreshCaptcha") ||
            text.contains("验证码") ||
            !text.contains("openWebLogin")
    }

    private fun shouldReplaceLoginUrl(loginUrl: String?): Boolean {
        val text = loginUrl?.trim().orEmpty()
        if (text.isBlank()) {
            return true
        }
        if (isUaaSource(text)) {
            return true
        }
        if (text.startsWith("http://", ignoreCase = true) ||
            text.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        return !text.contains("function login") ||
            !text.contains("source.putLoginHeader") ||
            !text.contains("solveUaaCaptcha")
    }
}
