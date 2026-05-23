package io.legado.app.help.source

import io.legado.app.data.entities.BookSource

object UaaLoginCompat {

    const val BOOK_SOURCE_URL = "https://www.uaa.com"

    val LOGIN_UI: String = """
        <js>
        JSON.stringify([
        {name:"账号",type:"text"},
        {name:"密码",type:"password"},
        {name:"验证码图片",type:"image",default:(function(){try{return (typeof freshCaptchaUrl=="function")?freshCaptchaUrl():String("https://www.uaa.com/email/captcha?t="+(new Date().getTime()))}catch(e){return "https://www.uaa.com/email/captcha?t="+(new Date().getTime())}})(),action:"refreshCaptcha()",style:{layout_flexGrow:1,layout_flexBasisPercent:1}},
        {name:"验证码",type:"text",default:(function(){try{return (typeof autoSolveUaaCaptcha=="function")?autoSolveUaaCaptcha((typeof currentCaptchaUrl=="function")?currentCaptchaUrl():String(source.get("uaaCaptchaImageUrl")||"")):""}catch(e){return ""}})()},
        {name:"刷新验证码",type:"button",action:"refreshCaptcha()",style:{layout_flexGrow:1,layout_flexBasisPercent:0.5}},
        {name:"网页登录兜底",type:"button",action:"openWebLogin()",style:{layout_flexGrow:1,layout_flexBasisPercent:0.5}}
        ]);
        </js>
    """.trimIndent()

    val LOGIN_URL: String = """
        <js>
        var uaaBase="https://www.uaa.com";
        var uaaLoginCandidates=[uaaBase,uaaBase+"/login",uaaBase+"/login.html",uaaBase+"/user/login",uaaBase+"/user/login.html",uaaBase+"/member/login",uaaBase+"/member/login.html",uaaBase+"/member.php?mod=logging&action=login"];
        function gv(m,k){try{return String(m.get(k)||"").trim()}catch(e){return ""}}
        function rememberPage(u,h){source.put("uaaLoginPageUrl",u);source.put("uaaLoginHtml",String(h||""));return String(h||"")}
        function absUaa(u,b){u=String(u||"").trim();if(!u)return "";if(u.indexOf("//")===0)return "https:"+u;if(/^https?:\/\//i.test(u))return u;try{return new java.net.URL(new java.net.URL(b||uaaBase),u).toString()}catch(e){return ""}}
        function addCandidate(a,u,b){u=absUaa(u,b);if(u&&a.indexOf(u)<0)a.push(u)}
        function responseBody(r){try{return String(r.body()||"")}catch(e){return String(r||"")}}
        function responseCode(r){try{return r.code()}catch(e){try{return r.statusCode()}catch(x){return 0}}}
        function responseHeader(r,n){try{return String(r.headers().get(n)||"")}catch(e){try{return String(r.header(n)||"")}catch(x){return ""}}}
        function responseUrl(r,f){try{return String(r.url()||f)}catch(e){return f}}
        function isCloudflare(body,code,cfHeader){body=String(body||"");cfHeader=String(cfHeader||"");return /challenge/i.test(cfHeader)||/_cf_chl_opt|cf[-_]?chl|challenge-platform|Just a moment|Checking your browser|cf_clearance|cf-ray/i.test(body)||((code==403||code==503)&&/cloudflare|Attention Required/i.test(body))}
        function isRequestFailure(e){var s=String(e&&e.message?e.message:e);return /Exception|Error|HTTP error|Status=|status code:|java\.|javax\.|okhttp3\.|org\.jsoup/i.test(s)}
        function isCloudflareError(e){var s=String(e&&e.message?e.message:e);return /cloudflare|cf[-_]?chl|challenge-platform|cf-mitigated|cf_clearance|cf-ray|_cf_chl_opt|Just a moment|Checking your browser|Attention Required/i.test(s)}
        function browserVerify(u,t){try{return java.startBrowserAwait(u,t||"Cloudflare")}catch(e){throw "登录失败：Cloudflare验证未完成："+e}}
        function postErrorNeedsBrowser(e,a){if(isCloudflareError(e))return true;var s=String(e&&e.message?e.message:e);if(!/Status=(403|503)|status code: (403|503)/i.test(s))return false;try{var h=String(java.ajax(uaaBase)||"");return isCloudflare(h,0,"")||(isRequestFailure(h)&&isCloudflareError(h))}catch(x){return isCloudflareError(x)}}
        function fetchHtml(u){var h=String(java.ajax(u)||"");if(isCloudflare(h,0,"")||(isRequestFailure(h)&&isCloudflareError(h))){var r=browserVerify(u,"Cloudflare");h=responseBody(r);if(!h||isCloudflare(h,responseCode(r),responseHeader(r,"cf-mitigated"))||(isRequestFailure(h)&&isCloudflareError(h)))throw "登录失败：Cloudflare验证未完成"}return h}
        function imgUaa(i){return String(i.attr("src")||i.attr("data-src")||i.attr("data-original")||i.attr("data-lazy-src")||i.attr("data-cfsrc")||i.attr("data-url")||"").trim()}
        function captchaImageScore(i,u){var p=i.parent(),s=String(u+" "+i.id()+" "+i.className()+" "+i.attr("alt")+" "+i.attr("title")+" "+i.attr("name")+" "+(p?p.id()+" "+p.className()+" "+p.text():"")).toLowerCase();if(/captcha|captche|verify|check_code|code|验证|驗證|结果|結果|result/.test(s))return 10;if(/logo|avatar|icon|loading|placeholder|banner|wechat|qq|二维码|qrcode|refresh/.test(s))return -1;return 1}
        function captchaFromStyle(d,b){var es=d.select("[style*=url]");for(var i=0;i<es.size();i++){var e=es.get(i),s=String(e.attr("style")||""),m=s.match(/url\(['"]?([^'")]+)['"]?\)/i);if(!m)continue;var u=absUaa(m[1],b),mark=String(u+" "+e.id()+" "+e.className()).toLowerCase();if(u&&/captcha|captche|verify|check_code|code|验证|驗證|结果|結果|result/.test(mark))return u}return ""}
        function cap(h){var b=String(source.get("uaaLoginPageUrl")||uaaBase),d=org.jsoup.Jsoup.parse(String(h||""),b),u=captchaFromStyle(d,b),direct=d.select("#login_captche_img,img[src*=email/captcha],.captcha_box img[id*=captche],.captcha_box img[src*=captcha]").first();if(!u&&direct)u=absUaa(imgUaa(direct),b);if(!u){var imgs=d.select("img[src],img[data-src],img[data-original],img[data-lazy-src],img[data-cfsrc],img[data-url]");for(var i=0;i<imgs.size();i++){var img=imgs.get(i),raw=imgUaa(img),abs=absUaa(raw,b);if(!abs)continue;var score=captchaImageScore(img,abs);if(score>=10){u=abs;break}}}if(u){source.put("uaaCaptchaImageUrl",u);source.put("uaaCaptchaUrl",u)}else{source.put("uaaCaptchaImageUrl","");source.put("uaaCaptchaUrl","")}return u}
        function discoverLoginCandidates(h,b){var d=org.jsoup.Jsoup.parse(String(h||""),b),a=[];var es=d.select("a[href*=login],a[href*=signin],a[href*=member],a[href*=user],form[action*=login],form[action*=signin],form[action*=member],form[action*=user]");for(var i=0;i<es.size();i++){var e=es.get(i);addCandidate(a,e.attr("href"),b);addCandidate(a,e.attr("action"),b)}return a}
        function webLoginClickerJs(){return ["(function(){",
        "if(window.__uaaLoginClicker)return;window.__uaaLoginClicker=true;",
        "function v(e){var r=e.getBoundingClientRect(),s=window.getComputedStyle(e);return r.width>0&&r.height>0&&s.display!='none'&&s.visibility!='hidden'}",
        "function t(e){return String(e.innerText||e.textContent||e.getAttribute('title')||e.getAttribute('aria-label')||e.getAttribute('alt')||e.className||e.id||e.getAttribute('src')||'')}",
        "function c(e){try{e.click();return true}catch(x){try{var ev=document.createEvent('MouseEvents');ev.initMouseEvent('click',true,true,window,1,0,0,0,0,false,false,false,false,0,null);e.dispatchEvent(ev);return true}catch(y){return false}}}",
        "function loginBox(){return document.querySelector('.login_box')}",
        "function ready(){var b=loginBox();return !!(b&&!/\\bhide\\b/.test(b.className)&&b.querySelector('input[name=login_password],input[type=password],input[name*=pwd]'))}",
        "function openLogin(){if(ready())return true;if(typeof toggleLogin=='function'){try{toggleLogin();return true}catch(e){}}var b=loginBox();if(b){b.className=String(b.className).replace(/\\bhide\\b/g,'');return true}return false}",
        "function closeNoise(){var ns=document.querySelectorAll('[class*=close],[id*=close],[class*=guanbi],[onclick*=close],[data-dismiss],.close,.btn-close,.modal-close,.layui-layer-close,.el-dialog__close,.van-popup__close-icon,button,a,span,i,img');for(var i=0;i<ns.length;i++){var e=ns[i];if(e.closest&&e.closest('.login_box,.regist_box'))continue;var tx=t(e);if(v(e)&&(/(关闭|close|×|跳过|知道|我知道|不再提示|guanbi)/i.test(tx)||/^\\s*x\\s*$/i.test(tx))){c(e)}}}",
        "function tick(){if(ready())return;closeNoise();if(openLogin())return;var all=document.querySelectorAll('a,button,div,span,li,[onclick*=login],[class*=login],[id*=login]');for(var j=0;j<all.length;j++){var el=all[j],tx=t(el).trim();if(v(el)&&/(登录|登錄|会员登录|會員登入|login|signin)/i.test(tx)&&!/(注册|註冊|忘记|忘記|退出|logout)/i.test(tx)&&!(el.closest&&el.closest('form'))){c(el);break}}}",
        "tick();setTimeout(tick,300);setInterval(tick,900);",
        "})()"].join("")}
        function webViewLoginJs(){return ["(function(){",
        "function v(e){var r=e.getBoundingClientRect(),s=window.getComputedStyle(e);return r.width>0&&r.height>0&&s.display!='none'&&s.visibility!='hidden'}",
        "function t(e){return String(e.innerText||e.textContent||e.getAttribute('title')||e.getAttribute('aria-label')||e.getAttribute('alt')||e.className||e.id||e.getAttribute('src')||'')}",
        "function c(e){try{e.click();return true}catch(x){try{var ev=document.createEvent('MouseEvents');ev.initMouseEvent('click',true,true,window,1,0,0,0,0,false,false,false,false,0,null);e.dispatchEvent(ev);return true}catch(y){return false}}}",
        "function loginBox(){return document.querySelector('.login_box')}",
        "function loginVisible(){var b=loginBox();return !!(b&&!/\\bhide\\b/.test(b.className)&&b.querySelector('input[name=login_password],input[type=password],input[name*=pwd]'))}",
        "function ready(){var b=loginBox();if(b&&!/\\bhide\\b/.test(b.className)&&(b.querySelector('#login_captche_img,.captcha_box img[src*=captcha],img[src*=email/captcha]')||b.querySelector('input[name=check_code]')))return true;var cap=document.querySelector('img[src*=captcha],img[src*=verify],img[src*=vcode],img[src*=code],img[data-src*=captcha],img[data-src*=verify],img[data-src*=vcode],img[data-src*=code],[style*=captcha],[style*=verify],[style*=vcode],[style*=code]');if(cap&&loginVisible())return true;return false}",
        "function openLogin(){if(loginVisible())return true;if(typeof toggleLogin=='function'){try{toggleLogin();return true}catch(e){}}var b=loginBox();if(b){b.className=String(b.className).replace(/\\bhide\\b/g,'');return true}return false}",
        "function closeNoise(){var ns=document.querySelectorAll('[class*=close],[id*=close],[class*=guanbi],[onclick*=close],[data-dismiss],.close,.btn-close,.modal-close,.layui-layer-close,.el-dialog__close,.van-popup__close-icon,button,a,span,i,img');for(var i=0;i<ns.length;i++){var e=ns[i];if(e.closest&&e.closest('.login_box,.regist_box'))continue;var tx=t(e);if(v(e)&&(/(关闭|close|×|跳过|知道|我知道|不再提示|guanbi)/i.test(tx)||/^\\s*x\\s*$/i.test(tx))){c(e)}}}",
        "function tick(){if(ready())return true;closeNoise();if(openLogin()&&ready())return true;var all=document.querySelectorAll('a,button,div,span,li,[onclick*=login],[class*=login],[id*=login]');for(var j=0;j<all.length;j++){var el=all[j],tx=t(el).trim();if(v(el)&&/(登录|登錄|会员登录|會員登入|login|signin)/i.test(tx)&&!/(注册|註冊|忘记|忘記|退出|logout)/i.test(tx)&&!(el.closest&&el.closest('form'))){c(el);break}}return ready()}",
        "if(tick())return document.documentElement.outerHTML;",
        "return tick()?document.documentElement.outerHTML:null;",
        "})()"].join("")}
        function webViewCaptchaUrl(u){try{return absUaa(String(java.webViewGetSource(null,u,webViewLoginJs(),"(?i).*(captcha|verify|vcode|checkcode|seccode|validate|yzm|code).*",false,1800)||""),u)}catch(e){source.put("uaaWebViewSourceError",String(e));return ""}}
        function webViewPage(u){try{var h=String(java.webView(null,u,webViewLoginJs(),false)||"");var cu=cap(h);if(!cu){cu=webViewCaptchaUrl(u);if(cu){source.put("uaaCaptchaImageUrl",cu);source.put("uaaCaptchaUrl",cu)}}return h}catch(e){source.put("uaaWebViewError",String(e));return ""}}
        function page(){var todo=uaaLoginCandidates.slice(0),seen={},first="";for(var i=0;i<todo.length&&i<20;i++){var u=todo[i];if(!u||seen[u])continue;seen[u]=true;source.put("uaaLoginPageUrl",u);var h=rememberPage(u,fetchHtml(u));if(!first)first=h;if(cap(h))return h;var wh=webViewPage(u);if(wh){h=rememberPage(u,wh);if(!first)first=h;if(cap(h)||String(source.get("uaaCaptchaImageUrl")||"").trim())return h}var more=discoverLoginCandidates(h,u);for(var j=0;j<more.length;j++)addCandidate(todo,more[j],u)}return first}
        function captchaUrl(){var h=page();return cap(h)||String(source.get("uaaCaptchaImageUrl")||"")}
        function directCaptchaUrl(){return uaaBase+"/email/captcha?t="+(new Date().getTime())}
        function cookieMapFromString(s){var m={},ps=String(s||"").split(";");for(var i=0;i<ps.length;i++){var p=ps[i],x=p.indexOf("=");if(x<1)continue;var k=String(p.substring(0,x)).trim(),v=String(p.substring(x+1)).trim();if(k)m[k]=v}return m}
        function mergeCookieStrings(){var m={},a=[];for(var i=0;i<arguments.length;i++){var c=cookieMapFromString(arguments[i]);for(var k in c){if(c.hasOwnProperty(k))m[k]=c[k]}}for(var n in m){if(m.hasOwnProperty(n))a.push(n+"="+m[n])}return a.join("; ")}
        function ck(cs){var a=[];try{var it=cs.entrySet().iterator();while(it.hasNext()){var e=it.next();a.push(e.getKey()+"="+e.getValue())}}catch(e){}return a.join("; ")}
        function rememberCookie(c){c=mergeCookieStrings(source.get("uaaLoginCookie"),c);if(c)source.put("uaaLoginCookie",c);return c}
        function storedUaaCookie(){var c=String(source.get("uaaLoginCookie")||"");try{c=mergeCookieStrings(c,cookie.getCookie(uaaBase))}catch(e){}return rememberCookie(c)}
        function rememberCookiesFromResponse(r){try{return rememberCookie(ck(r.cookies()))}catch(e){return storedUaaCookie()}}
        function withUaaCookie(h){h=h||{};var c=storedUaaCookie();if(c)h.Cookie=c;return h}
        function captchaDataUri(u){u=String(u||directCaptchaUrl());try{var r=java.get(u,withUaaCookie({"Referer":uaaBase+"/","User-Agent":java.getWebViewUA()}));rememberCookiesFromResponse(r);var ct=String(responseHeader(r,"Content-Type")||"image/png").split(";")[0]||"image/png";return "data:"+ct+";base64,"+java.base64Encode(r.bodyAsBytes())}catch(e){source.put("uaaCaptchaImageError",String(e));return u}}
        function rememberCaptchaUrl(u,r){u=String(u||"");r=String(r||"");if(u){source.put("uaaCaptchaImageUrl",u);source.put("uaaCaptchaUrl",u)}if(r)source.put("uaaCaptchaRequestUrl",r);return u}
        function currentCaptchaUrl(){var r=String(source.get("uaaCaptchaRequestUrl")||"")||directCaptchaUrl();return rememberCaptchaUrl(String(source.get("uaaCaptchaImageUrl")||"")||captchaDataUri(r),r)}
        function freshCaptchaUrl(){var r=directCaptchaUrl();return rememberCaptchaUrl(captchaDataUri(r),r)}
        function uaaCaptchaPrompt(){return "计算这张UAA验证码图片里的3个个位数字的加减乘公式，只返回最终整数结果，不要解释，不要返回公式"}
        function autoSolveUaaCaptcha(u){u=rememberCaptchaUrl(u||currentCaptchaUrl());if(!u)return "";var k="uaaCaptchaAnswer:"+u,c=String(source.get(k)||"");if(!c){try{c=String(java.aiCaptcha(u,uaaCaptchaPrompt())||"").trim();if(!/^-?\d+$/.test(c))c=""}catch(e){source.put("uaaCaptchaAiError",String(e));c=""}}if(c){source.put("uaaCaptchaAnswer",c);source.put(k,c)}return c}
        function refreshCaptcha(){var u=freshCaptchaUrl();var c=autoSolveUaaCaptcha(u);try{java.upLoginData({"验证码图片":u,"验证码":c})}catch(e){}return u}
        function solveUaaCaptcha(h,info){var c=gv(info,"验证码");if(!c){var u=String(source.get("uaaCaptchaImageUrl")||"")||cap(h)||currentCaptchaUrl();c=autoSolveUaaCaptcha(u);if(c)try{java.upLoginData({"验证码图片":u,"验证码":c})}catch(e){}}if(!c)throw "登录失败：请输入验证码";source.put("uaaCaptchaAnswer",c);return c}
        function openWebLogin(){java.showBrowser(uaaBase,null,webLoginClickerJs(),null)}
        function fallbackLoginHtml(){return "<div class='login_box'><form class='form_box'><input name='login_name'><input name='login_password' type='password'><input name='check_code'></form></div>"}
        function jsQuote(v){return JSON.stringify(String(v==null?"":v))}
        function shortText(s){return String(s||"").replace(/\s+/g," ").substring(0,180)}
        function webAutoLoginJs(u,p,c){return ["(function(){",
        "var U="+jsQuote(u)+",P="+jsQuote(p)+",C="+jsQuote(c)+";",
        "function txt(){return String(document.body&&document.body.innerText||'')}",
        "function done(o){o=o||{};o.url=location.href;o.title=document.title;o.cookie=document.cookie||'';if(!o.msg)o.msg=window.__uaaLastLoginResponse||short(txt());window.__uaaLoginResult=JSON.stringify(o);return window.__uaaLoginResult}",
        "function short(s){return String(s||'').replace(/\\s+/g,' ').substring(0,220)}",
        "if(window.__uaaLoginResult)return window.__uaaLoginResult;",
        "if(window._cf_chl_opt)return done({ok:false,cloudflare:true,msg:'Cloudflare'});",
        "function q(s){return document.querySelector(s)}",
        "function box(){return q('.login_box')}",
        "function visible(e){if(!e)return false;var r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&s.display!='none'&&s.visibility!='hidden'}",
        "function showBox(){var b=box();if(b&&/\\bhide\\b/.test(b.className))b.className=String(b.className).replace(/\\bhide\\b/g,'');if((!b||!visible(b))&&typeof toggleLogin=='function'){try{toggleLogin()}catch(e){}}return box()}",
        "function setVal(e,v){if(!e)return;try{var d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');d&&d.set?d.set.call(e,v):e.value=v}catch(x){e.value=v}try{e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}))}catch(y){}}",
        "function loginUrl(u){return /(login|signin|member|user|auth|email)/i.test(String(u||''))&&!/(captcha|captche|verify|vcode|checkcode)/i.test(String(u||''))}",
        "function verdict(body,status,url){var s=String(body||''),m=short(s);if(/_cf_chl_opt|cf[-_]?chl|challenge-platform|Just a moment|Checking your browser|cloudflare/i.test(s))return {ok:false,cloudflare:true,msg:'Cloudflare'};try{var o=JSON.parse(s),code=o.code,ok=o.success,msg=o.msg||o.message||o.error||o.data||m;if(ok===true||code===0||code===200||String(code)==='0'||String(code)==='200')return {ok:true,msg:String(msg||'登录成功')};if(ok===false||code!==undefined||o.status===false)return {ok:false,msg:String(msg||m)}}catch(e){}if(status>=400)return {ok:false,msg:'接口返回'+status+'：'+m};if(/登录成功|登錄成功|success/i.test(s)&&!/失败|失敗|错误|錯誤|验证码|驗證碼|密码|密碼|invalid|incorrect/i.test(s))return {ok:true,msg:m||'登录成功'};if(/验证码|驗證碼|密码|密碼|错误|錯誤|失败|失敗|invalid|incorrect|not found|Whitelabel/i.test(s))return {ok:false,msg:m};return null}",
        "function analyze(body,status,url){if(!loginUrl(url))return;window.__uaaLastLoginResponse=short(url+' '+status+' '+String(body||''));var v=verdict(body,status,url);if(v)done(v)}",
        "if(!window.__uaaPatchedLoginNet){window.__uaaPatchedLoginNet=true;var of=window.fetch;if(of){window.fetch=function(){var a=arguments,u=String(a[0]&&a[0].url||a[0]||'');return of.apply(this,a).then(function(r){try{if(loginUrl(u))r.clone().text().then(function(t){analyze(t,r.status,u)}).catch(function(){})}catch(e){}return r}).catch(function(e){if(loginUrl(u))done({ok:false,msg:String(e)});throw e})}};var xo=XMLHttpRequest.prototype.open,xs=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.open=function(m,u){this.__uaaUrl=String(u||'');return xo.apply(this,arguments)};XMLHttpRequest.prototype.send=function(){var x=this;try{x.addEventListener('loadend',function(){try{analyze(x.responseText,x.status,x.__uaaUrl)}catch(e){}})}catch(e){}return xs.apply(this,arguments)}}",
        "if(!window.__uaaLoginStarted){var b=showBox(),f=q('.login_box form')||q('form'),n=q('input[name=login_name]'),pw=q('input[name=login_password],input[type=password]'),cc=q('input[name=check_code]');if(!(n&&pw&&cc))return null;setVal(n,U);setVal(pw,P);setVal(cc,C);window.__uaaLoginStarted=true;setTimeout(function(){if(!window.__uaaLoginResult){var t=txt();if(/退出|登出|会员中心|會員中心|个人中心|個人中心|logout/i.test(t))done({ok:true,msg:'页面显示已登录'});else done({ok:false,msg:window.__uaaLastLoginResponse||short(t)||'登录无响应'})}},9000);setTimeout(function(){try{var ev={preventDefault:function(){},stopPropagation:function(){},target:f,currentTarget:f};if(typeof login=='function'){var r=login(ev,'');if(r&&typeof r.then=='function')r.then(function(v){if(v!==undefined){var vd=verdict(String(v),200,'login');if(vd)done(vd)}}).catch(function(e){done({ok:false,msg:String(e)})})}else if(f){var btn=f.querySelector('button[type=submit],.login_btn');if(btn)btn.click();else f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}))}else done({ok:false,msg:'未找到登录表单'})}catch(e){done({ok:false,msg:'登录脚本异常：'+e})}},80)}",
        "return null",
        "})()"].join("")}
        function parseWebLoginResult(raw){try{return JSON.parse(String(raw||""))}catch(e){return null}}
        function runWebAutoLogin(u,p,c){return String(java.webView(null,uaaBase,webAutoLoginJs(u,p,c),false)||"")}
        function webAutoLogin(u,p,c){var raw="",o=null;try{raw=runWebAutoLogin(u,p,c)}catch(e){if(!isCloudflareError(e))throw e;o={cloudflare:true,msg:String(e)}}if(!o)o=parseWebLoginResult(raw);source.put("uaaWebLoginResult",raw);if(o&&o.cookie)rememberCookie(o.cookie);var ch=storedUaaCookie();if(o&&o.ok){if(ch)source.putLoginHeader(JSON.stringify({Cookie:ch}));java.toast("UAA登录成功");return true}var cfText=o?String(o.msg||""):raw;if((o&&o.cloudflare)||isCloudflare(cfText,0,"")||(isRequestFailure(cfText)&&isCloudflareError(cfText))){browserVerify(uaaBase,"Cloudflare");raw=runWebAutoLogin(u,p,c);source.put("uaaWebLoginResult",raw);o=parseWebLoginResult(raw);if(o&&o.cookie)rememberCookie(o.cookie);ch=storedUaaCookie();if(o&&o.ok){if(ch)source.putLoginHeader(JSON.stringify({Cookie:ch}));java.toast("UAA登录成功");return true}}var msg=o&&o.msg?o.msg:shortText(raw);if(o&&o.cloudflare)throw "登录失败：Cloudflare验证未完成";throw "登录失败："+(msg||"网页登录无结果")}
        function login(){var info=typeof result!="undefined"?result:source.getLoginInfoMap(),u=gv(info,"账号"),p=gv(info,"密码");if(!u||!p)throw "登录失败：请输入账号和密码";var c=solveUaaCaptcha("",info);webAutoLogin(u,p,c)}
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
            text.contains("default:captchaUrl()") ||
            text.contains("source.get(\"uaaCaptchaUrl\")") ||
            !text.contains("验证码图片") ||
            !text.contains("type:\"image\"") ||
            !text.contains("name:\"验证码\"") ||
            !text.contains("autoSolveUaaCaptcha") ||
            !text.contains("directCaptchaUrl") ||
            !text.contains("freshCaptchaUrl") ||
            !text.contains("/email/captcha?t=") ||
            !text.contains("refreshCaptcha") ||
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
        if (text.contains("AI验证码")) {
            return true
        }
        if (text.contains("absUrl")) {
            return true
        }
        if (text.contains("loginLike&&score>0&&!fallback")) {
            return true
        }
        return !text.contains("function login") ||
            !text.contains("source.putLoginHeader") ||
            !text.contains("solveUaaCaptcha") ||
            !text.contains("gv(info,\"验证码\")") ||
            !text.contains("function refreshCaptcha") ||
            !text.contains("java.upLoginData") ||
            !text.contains("function isCloudflare") ||
            !text.contains("function isRequestFailure") ||
            !text.contains("function isCloudflareError") ||
            !text.contains("function postErrorNeedsBrowser") ||
            !text.contains("java.startBrowserAwait") ||
            !text.contains("function absUaa") ||
            !text.contains("uaaLoginCandidates") ||
            !text.contains("function directCaptchaUrl") ||
            !text.contains("function currentCaptchaUrl") ||
            !text.contains("function freshCaptchaUrl") ||
            !text.contains("function captchaDataUri") ||
            !text.contains("function autoSolveUaaCaptcha") ||
            !text.contains("java.aiCaptcha") ||
            !text.contains("java.base64Encode") ||
            !text.contains("bodyAsBytes") ||
            !text.contains("3个个位数字") ||
            !text.contains("只返回最终整数结果") ||
            !text.contains("function rememberCookiesFromResponse") ||
            !text.contains("function withUaaCookie") ||
            !text.contains("function fallbackLoginHtml") ||
            !text.contains("function webAutoLoginJs") ||
            !text.contains("function webAutoLogin") ||
            !text.contains("java.webView(null,uaaBase,webAutoLoginJs") ||
            !text.contains("login(ev,'')") ||
            !text.contains("browserVerify(uaaBase,\"Cloudflare\")") ||
            !text.contains("captchaImageScore") ||
            !text.contains("function webViewPage") ||
            !text.contains("function webViewLoginJs") ||
            !text.contains("function webViewCaptchaUrl") ||
            !text.contains("function webLoginClickerJs") ||
            !text.contains("window.__uaaLoginClicker") ||
            !text.contains("var cfText=o?String(o.msg||\"\"):raw") ||
            !text.contains("isCloudflare(cfText,0,\"\")") ||
            !text.contains("typeof toggleLogin=='function'") ||
            !text.contains("e.closest('.login_box,.regist_box')") ||
            !text.contains("#login_captche_img") ||
            !text.contains("input[name=login_name]") ||
            !text.contains("isRequestFailure(h)&&isCloudflareError(h)") ||
            !text.contains("uaaCaptchaImageUrl") ||
            !text.contains("return tick()?document.documentElement.outerHTML:null") ||
            text.contains("function refreshCaptcha(){var u=captchaUrl()") ||
            text.contains("var h=page(),c=solveUaaCaptcha") ||
            text.contains("uaaBase+\"/email/login\"") ||
            text.contains("java.post(a,body,headers)") ||
            text.contains("isCloudflare(raw,0,\"\")") ||
            text.contains("addCandidate(a,uaaBase+\"/login\",b)") ||
            text.contains("Status=403|Status=503")
    }
}
