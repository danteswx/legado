package io.legado.app.ui.login

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class SourceLoginCompatibilityTest {

    @Test
    fun sourceLoginViewModelUpgradesLegacyUaaSourceBeforeReadingLoginInfo() {
        val viewModelSource = repoFile(
            "app/src/main/java/io/legado/app/ui/login/SourceLoginViewModel.kt"
        ).readText()

        val compatCall = viewModelSource.indexOf("applySourceLoginCompatIfNeeded()")
        val headerRead = viewModelSource.indexOf("source?.getHeaderMap(true)")

        assertTrue("UAA compat should run during login initialization", compatCall >= 0)
        assertTrue("UAA compat should run before login info is derived", compatCall < headerRead)
        assertTrue(viewModelSource.contains("UaaLoginCompat.apply(bookSource)"))
        assertTrue(viewModelSource.contains("appDb.bookSourceDao.update(bookSource)"))
    }

    @Test
    fun explorePageTreatsUaaAsLoginCapableEvenWhenSourcePartIsStale() {
        val exploreSource = repoFile(
            "app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt"
        ).readText()

        assertTrue(exploreSource.contains("UaaLoginCompat.isUaaSource"))
        assertTrue(exploreSource.contains("selectedDiscoverSourcePart?.canOpenSourceLogin() == true"))
    }

    @Test
    fun uaaCompatShowsManualCaptchaImageInLoginUi() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUi = compatSource.substringAfter("val LOGIN_UI: String =")
            .substringBefore("val LOGIN_URL: String =")

        assertTrue(compatSource.contains("shouldReplaceLoginUi"))
        assertTrue(compatSource.contains("AI识别验证码"))
        assertTrue(compatSource.contains("验证码"))
        assertTrue(loginUi.contains("验证码图片"))
        assertTrue(loginUi.contains("type:\"image\""))
        assertTrue(loginUi.contains("name:\"验证码\",type:\"text\""))
        assertTrue(loginUi.contains("refreshCaptcha()"))
        assertTrue(loginUi.contains("openWebLogin"))
        assertTrue(loginUi.contains("uaaCaptchaImageUrl"))
        assertTrue(loginUi.contains("directCaptchaUrl"))
        assertTrue(loginUi.contains("freshCaptchaUrl"))
        assertTrue(loginUi.contains("autoSolveUaaCaptcha"))
        assertTrue(loginUi.contains("/email/captcha?t="))
        assertFalse(loginUi.contains("source.get(\"uaaCaptchaUrl\")"))
        assertFalse(loginUi.contains("default:captchaUrl()"))
        assertFalse(loginUi.contains("AI识别验证码"))
        assertTrue(compatSource.contains("!text.contains(\"type:\\\"image\\\"\")"))
        assertTrue(compatSource.contains("text.contains(\"source.get(\\\"uaaCaptchaUrl\\\")\")"))
        assertTrue(compatSource.contains("text.contains(\"default:captchaUrl()\")"))
    }

    @Test
    fun loginRowUiSupportsInlineImageRows() {
        val rowUiSource = repoFile(
            "app/src/main/java/io/legado/app/data/entities/rule/RowUi.kt"
        ).readText()
        val rowUiFormSource = repoFile(
            "app/src/main/java/io/legado/app/ui/widget/RowUiForm.kt"
        ).readText()
        val dialogSource = repoFile(
            "app/src/main/java/io/legado/app/ui/login/SourceLoginDialog.kt"
        ).readText()

        assertTrue(rowUiSource.contains("const val image = \"image\""))
        assertTrue(rowUiFormSource.contains("RowUi.Type.image"))
        assertTrue(rowUiFormSource.contains("createImageRow"))
        assertTrue(dialogSource.contains("Type.image"))
        assertTrue(dialogSource.contains("RowUiViewFactory.loadImageRow"))
        assertTrue(dialogSource.contains("loginInfo[rowUi.name]?.takeIf { it.isNotEmpty() }"))
        assertTrue(dialogSource.contains("shouldPreferFreshDefault(rowUi)"))
        assertTrue(dialogSource.contains("name.contains(\"验证码\")"))
    }

    @Test
    fun uaaCompatUsesManualCaptchaAndCloudflareBrowserVerification() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUrl = compatSource.substringAfter("val LOGIN_URL: String =")
            .substringBefore("fun isUaaSource")
        val loginBody = loginUrl.substringAfter("function login()")

        assertTrue(loginUrl.contains("function solveUaaCaptcha"))
        assertTrue(loginUrl.contains("gv(info,\"验证码\")"))
        assertTrue(loginUrl.contains("function refreshCaptcha"))
        assertTrue(loginUrl.contains("java.upLoginData"))
        assertTrue(loginUrl.contains("function isCloudflare"))
        assertTrue(loginUrl.contains("function isCloudflareError"))
        assertTrue(loginUrl.contains("java.startBrowserAwait"))
        assertTrue(loginUrl.contains("function postErrorNeedsBrowser"))
        assertTrue(loginUrl.contains("java.ajax(uaaBase)"))
        assertTrue(loginUrl.contains("function webLoginClickerJs"))
        assertTrue(loginUrl.contains("java.showBrowser(uaaBase,null,webLoginClickerJs(),null)"))
        assertTrue(loginUrl.contains("window.__uaaLoginClicker"))
        assertTrue(loginUrl.contains("typeof toggleLogin=='function'"))
        assertTrue(loginUrl.contains("document.querySelector('.login_box')"))
        assertTrue(loginUrl.contains("e.closest('.login_box,.regist_box')"))
        assertTrue(loginUrl.contains("setInterval(tick"))
        assertTrue(loginUrl.contains("cf-mitigated"))
        assertTrue(loginUrl.contains("_cf_chl_opt"))
        assertTrue(loginUrl.contains("Just a moment"))
        assertTrue(loginUrl.contains("function webAutoLoginJs"))
        assertTrue(loginUrl.contains("function webAutoLogin"))
        assertTrue(loginUrl.contains("login(ev,'')"))
        assertTrue(loginUrl.contains("java.webView(null,uaaBase,webAutoLoginJs(u,p,c),false)"))
        assertTrue(loginUrl.contains("browserVerify(uaaBase,\"Cloudflare\")"))
        assertTrue(loginUrl.contains("function rememberCookiesFromResponse"))
        assertTrue(loginUrl.contains("function withUaaCookie"))
        assertTrue(
            loginUrl.substringAfter("function captchaDataUri")
                .substringBefore("function rememberCaptchaUrl")
                .contains("rememberCookiesFromResponse(r)")
        )
        assertTrue(loginBody.contains("webAutoLogin(u,p,c)"))
        assertFalse(loginUrl.contains("uaaBase+\"/email/login\""))
        assertFalse(loginUrl.contains("java.post(a,body,headers)"))
        assertFalse(loginBody.contains("var h=page()"))
        assertFalse(loginUrl.contains("AI验证码"))
    }

    @Test
    fun loginUiDefaultEvaluationCanUseLoginJsExtensions() {
        val dialogSource = repoFile(
            "app/src/main/java/io/legado/app/ui/login/SourceLoginDialog.kt"
        ).readText()
        val evalUiBody = dialogSource.substringAfter("suspend fun evalUiJs")
            .substringBefore("fun loginUi")

        assertTrue(evalUiBody.contains("put(\"java\", sourceLoginJsExtensions)"))
    }

    @Test
    fun uaaWebAutoLoginDoesNotTreatSuccessCookiesAsCloudflare() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUrl = compatSource.substringAfter("val LOGIN_URL: String =")
            .substringBefore("fun isUaaSource")
        val webAutoLoginBody = loginUrl.substringAfter("function webAutoLogin(u,p,c)")
            .substringBefore("function login()")

        val successIndex = webAutoLoginBody.indexOf("if(o&&o.ok)")
        val cloudflareIndex = webAutoLoginBody.indexOf("browserVerify(uaaBase,\"Cloudflare\")")

        assertTrue("UAA web auto login should handle success before Cloudflare fallback", successIndex >= 0)
        assertTrue("UAA web auto login should still keep Cloudflare fallback", cloudflareIndex >= 0)
        assertTrue(
            "A successful result may include cf_clearance in document.cookie; do not scan the whole result JSON before success handling",
            successIndex < cloudflareIndex
        )
        assertTrue(webAutoLoginBody.contains("var cfText=o?String(o.msg||\"\"):raw"))
        assertTrue(webAutoLoginBody.contains("isCloudflare(cfText,0,\"\")"))
        assertFalse(webAutoLoginBody.contains("isCloudflare(raw,0,\"\")"))
    }

    @Test
    fun uaaRefreshCaptchaDoesNotOpenBrowserVerification() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUrl = compatSource.substringAfter("val LOGIN_URL: String =")
            .substringBefore("fun isUaaSource")
        val refreshBody = loginUrl.substringAfter("function refreshCaptcha()")
            .substringBefore("function solveUaaCaptcha")

        assertTrue(loginUrl.contains("function directCaptchaUrl"))
        assertTrue(loginUrl.contains("function freshCaptchaUrl"))
        assertTrue(loginUrl.contains("function captchaDataUri"))
        assertTrue(loginUrl.contains("function autoSolveUaaCaptcha"))
        assertTrue(loginUrl.contains("java.aiCaptcha"))
        assertTrue(loginUrl.contains("java.base64Encode"))
        assertTrue(loginUrl.contains("bodyAsBytes"))
        assertTrue(loginUrl.contains("3个个位数字"))
        assertTrue(loginUrl.contains("加减乘"))
        assertTrue(loginUrl.contains("只返回最终整数结果"))
        assertTrue(loginUrl.contains("source.put(\"uaaCaptchaAnswer\",c)"))
        assertTrue(refreshBody.contains("var u=freshCaptchaUrl()"))
        assertTrue(refreshBody.contains("autoSolveUaaCaptcha(u)"))
        assertTrue(refreshBody.contains("java.upLoginData({\"验证码图片\":u,\"验证码\":c})"))
        assertFalse(refreshBody.contains("captchaUrl()"))
        assertFalse(refreshBody.contains("page()"))
        assertFalse(refreshBody.contains("fetchHtml"))
        assertFalse(refreshBody.contains("browserVerify"))
        assertFalse(refreshBody.contains("startBrowserAwait"))
        assertFalse(refreshBody.contains("showBrowser"))
    }

    @Test
    fun uaaCloudflareErrorDetectorIsNotUsedForPlainResponseBodies() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUrl = compatSource.substringAfter("val LOGIN_URL: String =")
            .substringBefore("fun isUaaSource")
        val fetchBody = loginUrl.substringAfter("function fetchHtml(u)")
            .substringBefore("function imgUaa")
        val webAutoLoginBody = loginUrl.substringAfter("function webAutoLogin(u,p,c)")
            .substringBefore("function login()")

        assertTrue(loginUrl.contains("function isRequestFailure"))
        assertTrue(fetchBody.contains("isRequestFailure(h)&&isCloudflareError(h)"))
        assertTrue(fetchBody.contains("if(isCloudflare(h,0,\"\"))"))
        assertFalse(fetchBody.contains("||isCloudflareError(h)"))
        assertFalse(loginUrl.contains("Status=403|Status=503"))
        assertFalse(loginUrl.contains("status code: 403|status code: 503"))
        assertTrue(loginUrl.contains("function postErrorNeedsBrowser"))
        assertTrue(loginUrl.contains("java.ajax(uaaBase)"))
        assertTrue(webAutoLoginBody.contains("var cfText=o?String(o.msg||\"\"):raw"))
        assertTrue(webAutoLoginBody.contains("isCloudflare(cfText,0,\"\")"))
        assertFalse(webAutoLoginBody.contains("isCloudflare(raw,0,\"\")"))
        assertTrue(loginUrl.contains("isCloudflare(h,0,\"\")"))
        assertFalse(webAutoLoginBody.contains("isCloudflareError(rb)"))
    }

    @Test
    fun uaaCompatAvoidsJsoupAbsUrlForOptionalCaptchaAndFormAttributes() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUrl = compatSource.substringAfter("val LOGIN_URL: String =")
            .substringBefore("fun isUaaSource")

        assertTrue(loginUrl.contains("function absUaa"))
        assertTrue(loginUrl.contains("attr(\"data-src\")"))
        assertTrue(loginUrl.contains("attr(\"data-original\")"))
        assertTrue(loginUrl.contains("attr(\"data-lazy-src\")"))
        assertTrue(loginUrl.contains("#login_captche_img"))
        assertTrue(loginUrl.contains("img[src*=email/captcha]"))
        assertTrue(loginUrl.contains("input[name=check_code]"))
        assertTrue(loginUrl.contains("input[name=login_name]"))
        assertTrue(loginUrl.contains("input[name=login_password],input[type=password]"))
        assertTrue(loginUrl.contains("input[name=check_code]"))
        assertTrue(loginUrl.contains("source.put(\"uaaCaptchaImageUrl\",u)"))
        assertFalse(loginUrl.contains("loginLike&&score>0&&!fallback"))
        assertFalse(loginUrl.contains("if(!u)u=fallback"))
        assertFalse(loginUrl.contains("i.absUrl(\"src\")"))
        assertFalse(loginUrl.contains("f.absUrl(\"action\")"))
        assertTrue(compatSource.contains("!text.contains(\"function absUaa\")"))
        assertTrue(compatSource.contains("!text.contains(\"uaaLoginCandidates\")"))
        assertTrue(compatSource.contains("!text.contains(\"captchaImageScore\")"))
        assertTrue(compatSource.contains("!text.contains(\"function webViewPage\")"))
        assertTrue(compatSource.contains("!text.contains(\"function webViewLoginJs\")"))
        assertTrue(compatSource.contains("!text.contains(\"function webViewCaptchaUrl\")"))
        assertTrue(compatSource.contains("!text.contains(\"function webLoginClickerJs\")"))
        assertTrue(compatSource.contains("!text.contains(\"function isCloudflareError\")"))
        assertTrue(compatSource.contains("!text.contains(\"return tick()?document.documentElement.outerHTML:null\")"))
        assertTrue(compatSource.contains("text.contains(\"absUrl\")"))
    }

    @Test
    fun uaaCompatRefreshesLoginPageAndUsesBroadCaptchaImageCandidates() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUrl = compatSource.substringAfter("val LOGIN_URL: String =")
            .substringBefore("fun isUaaSource")
        val loginBody = loginUrl.substringAfter("function login()")

        assertTrue(loginUrl.contains("uaaLoginCandidates"))
        assertTrue(loginUrl.contains("source.put(\"uaaLoginPageUrl\""))
        assertTrue(loginUrl.contains("img[src],img[data-src],img[data-original]"))
        assertTrue(loginUrl.contains("captchaImageScore"))
        assertTrue(loginUrl.contains("captchaFromStyle"))
        assertTrue(loginUrl.contains("function webViewPage"))
        assertTrue(loginUrl.contains("function webViewLoginJs"))
        assertTrue(loginUrl.contains("function webViewCaptchaUrl"))
        assertTrue(loginUrl.contains("java.webView(null,u"))
        assertTrue(loginUrl.contains("java.webViewGetSource(null,u"))
        assertTrue(loginUrl.contains("querySelectorAll('[class*=close]"))
        assertTrue(loginUrl.contains("登录|登錄|会员登录|會員登入|login|signin"))
        assertTrue(loginUrl.contains("function ready()"))
        assertTrue(loginUrl.contains("[onclick*=close]"))
        assertTrue(loginUrl.contains("if(ready())return document.documentElement.outerHTML"))
        assertTrue(loginUrl.contains("return tick()?document.documentElement.outerHTML:null"))
        assertTrue(loginUrl.contains("!(el.closest&&el.closest('form'))"))
        assertTrue(loginUrl.contains("document.documentElement.outerHTML"))
        assertTrue(loginUrl.contains("function page()"))
        assertFalse(loginBody.contains("var h=page()"))
        assertFalse(loginBody.contains("source.get(\"uaaLoginHtml\")"))
    }

    private fun repoFile(path: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, path) }
            .firstOrNull { it.exists() }
            ?: error("$path not found")
    }
}
