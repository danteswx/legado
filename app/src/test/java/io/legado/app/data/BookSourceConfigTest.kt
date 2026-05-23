package io.legado.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BookSourceConfigTest {

    @Test
    fun shareBookSourceJsonIsUtf8WithoutBom() {
        val bytes = repoFile("tests/shareBookSource.json").readBytes()

        assertFalse(
            "shareBookSource.json should not start with UTF-8 BOM because the app import path treats it as invalid",
            bytes.take(3).toByteArray().contentEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        )
    }

    @Test
    fun uaaSourceDetectsCloudflareAndOpensBrowserVerification() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val uaaSource = sourceObject(sourceText, """"bookSourceUrl": "https://www.uaa.com"""")
        val loginCheckJs = fieldValue(uaaSource, "loginCheckJs")

        assertTrue("UAA source should exist", uaaSource.isNotBlank())
        assertTrue(uaaSource.contains(""""concurrentRate": "1/3000""""))
        assertFalse("UAA loginCheckJs should not be blank", loginCheckJs.isBlank())
        assertTrue(loginCheckJs.contains("startBrowserAwait"))
        assertFalse(loginCheckJs.contains("removeCookie"))
        assertTrue(loginCheckJs.contains("cf-mitigated"))
        assertTrue(loginCheckJs.contains("_cf_chl_opt"))
        assertTrue(loginCheckJs.contains("cf_chl"))
        assertTrue(loginCheckJs.contains("Just a moment"))
    }

    @Test
    fun uaaBookInfoCoverUsesLazyImageAndFallsBackToListCover() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val uaaSource = sourceObject(sourceText, """"bookSourceUrl": "https://www.uaa.com"""")
        val coverUrlRule = fieldValue(uaaSource, "coverUrl")

        assertTrue("UAA source should exist", uaaSource.isNotBlank())
        assertTrue(coverUrlRule.contains("data-src"))
        assertTrue(coverUrlRule.contains("data-original"))
        assertTrue(coverUrlRule.contains("book.coverUrl"))
        assertTrue(coverUrlRule.contains("data:image"))
        assertTrue(coverUrlRule.contains("placeholder"))
        assertTrue(coverUrlRule.contains("src ||"))
        assertTrue(coverUrlRule.contains("og:image"))
        assertTrue(coverUrlRule.contains("coverUrl"))
    }

    @Test
    fun uaaSourceUsesInlineManualCaptchaWithWebFallback() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val uaaSource = sourceObject(sourceText, """"bookSourceUrl": "https://www.uaa.com"""")
        val loginUi = fieldValue(uaaSource, "loginUi")
        val loginUrl = fieldValue(uaaSource, "loginUrl")
        val decodedLoginUrl = unescapeJsonString(loginUrl)
        val refreshCaptchaBody = decodedLoginUrl.substringAfter("function refreshCaptcha()")
            .substringBefore("function solveUaaCaptcha")
        val captchaDataUriBody = decodedLoginUrl.substringAfter("function captchaDataUri")
            .substringBefore("function rememberCaptchaUrl")

        assertTrue("UAA source should exist", uaaSource.isNotBlank())
        assertFalse("UAA loginUi should not be blank", loginUi.isBlank())
        assertTrue(loginUi.contains("账号"))
        assertTrue(loginUi.contains("密码"))
        assertTrue("UAA captcha image should be visible in login UI", loginUi.contains("验证码图片"))
        assertTrue("UAA login UI should use inline image row", loginUi.contains("type:\"image\""))
        assertTrue("UAA login UI should include manual captcha input", loginUi.contains("name:\"验证码\",type:\"text\""))
        assertTrue("UAA login UI should include captcha refresh", loginUi.contains("refreshCaptcha()"))
        assertTrue("UAA login UI should use only fresh cached captcha before the dialog renders", loginUi.contains("uaaCaptchaImageUrl"))
        assertTrue("UAA login UI should render the captcha image without probing the login page", loginUi.contains("directCaptchaUrl"))
        assertTrue("UAA login UI should generate a new captcha each time it opens", loginUi.contains("freshCaptchaUrl"))
        assertTrue("UAA login UI should auto-fill captcha from AI recognition", loginUi.contains("autoSolveUaaCaptcha"))
        assertTrue("UAA login UI should use the known captcha image endpoint", loginUi.contains("/email/captcha?t="))
        assertFalse("UAA login UI must not show stale old captcha cache", loginUi.contains("source.get(\"uaaCaptchaUrl\")"))
        assertFalse("UAA login UI must not fetch captcha before the dialog renders", loginUi.contains("default:captchaUrl()"))
        assertFalse("UAA should not show a manual AI captcha button", loginUi.contains("AI识别验证码"))
        assertTrue(loginUi.contains("openWebLogin"))
        assertTrue(decodedLoginUrl.contains("function login"))
        assertTrue(decodedLoginUrl.contains("solveUaaCaptcha"))
        assertTrue(decodedLoginUrl.contains("gv(info,\"验证码\")"))
        assertTrue(decodedLoginUrl.contains("function refreshCaptcha"))
        assertTrue(decodedLoginUrl.contains("function directCaptchaUrl"))
        assertTrue(decodedLoginUrl.contains("function freshCaptchaUrl"))
        assertTrue(decodedLoginUrl.contains("function captchaDataUri"))
        assertTrue(decodedLoginUrl.contains("function autoSolveUaaCaptcha"))
        assertTrue(decodedLoginUrl.contains("java.aiCaptcha"))
        assertTrue(decodedLoginUrl.contains("java.base64Encode"))
        assertTrue(decodedLoginUrl.contains("bodyAsBytes"))
        assertTrue(decodedLoginUrl.contains("3个个位数字"))
        assertTrue(decodedLoginUrl.contains("加减乘"))
        assertTrue(decodedLoginUrl.contains("只返回最终整数结果"))
        assertTrue(decodedLoginUrl.contains("source.put(\"uaaCaptchaAnswer\",c)"))
        assertTrue(refreshCaptchaBody.contains("var u=freshCaptchaUrl()"))
        assertTrue(refreshCaptchaBody.contains("autoSolveUaaCaptcha(u)"))
        assertTrue(refreshCaptchaBody.contains("java.upLoginData({\"验证码图片\":u,\"验证码\":c})"))
        assertFalse("Refreshing captcha must not open browser verification", refreshCaptchaBody.contains("startBrowserAwait"))
        assertFalse("Refreshing captcha must not open web login fallback", refreshCaptchaBody.contains("showBrowser"))
        assertFalse("Refreshing captcha must not fetch login pages", refreshCaptchaBody.contains("page()"))
        assertTrue(decodedLoginUrl.contains("java.upLoginData"))
        assertFalse(decodedLoginUrl.contains("AI验证码"))
        assertTrue(decodedLoginUrl.contains("function absUaa"))
        assertTrue(decodedLoginUrl.contains("uaaLoginCandidates"))
        assertTrue(decodedLoginUrl.contains("source.put(\"uaaLoginPageUrl\""))
        assertTrue(decodedLoginUrl.contains("attr(\"data-src\")"))
        assertTrue(decodedLoginUrl.contains("attr(\"data-original\")"))
        assertTrue(decodedLoginUrl.contains("attr(\"data-lazy-src\")"))
        assertTrue(decodedLoginUrl.contains("#login_captche_img"))
        assertTrue(decodedLoginUrl.contains("img[src*=email/captcha]"))
        assertTrue(decodedLoginUrl.contains("input[name=check_code]"))
        assertTrue(decodedLoginUrl.contains("input[name=login_name]"))
        assertTrue(decodedLoginUrl.contains("input[name=login_password],input[type=password]"))
        assertTrue(decodedLoginUrl.contains("input[name=check_code]"))
        assertTrue(decodedLoginUrl.contains("source.put(\"uaaCaptchaImageUrl\",u)"))
        assertFalse("UAA captcha parser must not fall back to ordinary page images", decodedLoginUrl.contains("loginLike&&score>0&&!fallback"))
        assertFalse("UAA captcha parser must not use a generic image fallback", decodedLoginUrl.contains("if(!u)u=fallback"))
        assertTrue(decodedLoginUrl.contains("captchaImageScore"))
        assertTrue(decodedLoginUrl.contains("captchaFromStyle"))
        assertTrue(decodedLoginUrl.contains("function webViewPage"))
        assertTrue(decodedLoginUrl.contains("function webViewLoginJs"))
        assertTrue(decodedLoginUrl.contains("function webViewCaptchaUrl"))
        assertTrue(decodedLoginUrl.contains("function webLoginClickerJs"))
        assertTrue(decodedLoginUrl.contains("java.showBrowser(uaaBase,null,webLoginClickerJs(),null)"))
        assertTrue(decodedLoginUrl.contains("window.__uaaLoginClicker"))
        assertTrue(decodedLoginUrl.contains("typeof toggleLogin=='function'"))
        assertTrue(decodedLoginUrl.contains("document.querySelector('.login_box')"))
        assertTrue(decodedLoginUrl.contains("e.closest('.login_box,.regist_box')"))
        assertTrue(decodedLoginUrl.contains("setInterval(tick"))
        assertTrue(decodedLoginUrl.contains("java.webView(null,u"))
        assertTrue(decodedLoginUrl.contains("java.webViewGetSource(null,u"))
        assertTrue(decodedLoginUrl.contains("querySelectorAll('[class*=close]"))
        assertTrue(decodedLoginUrl.contains("登录|登錄|会员登录|會員登入|login|signin"))
        assertTrue(decodedLoginUrl.contains("function ready()"))
        assertTrue(decodedLoginUrl.contains("[onclick*=close]"))
        assertTrue(decodedLoginUrl.contains("if(ready())return document.documentElement.outerHTML"))
        assertTrue(decodedLoginUrl.contains("return tick()?document.documentElement.outerHTML:null"))
        assertTrue(decodedLoginUrl.contains("!(el.closest&&el.closest('form'))"))
        assertTrue(decodedLoginUrl.contains("document.documentElement.outerHTML"))
        assertTrue(decodedLoginUrl.contains("function fallbackLoginHtml"))
        assertTrue(decodedLoginUrl.contains("function webAutoLoginJs"))
        assertTrue(decodedLoginUrl.contains("function webAutoLogin"))
        assertTrue(decodedLoginUrl.contains("login(ev,'')"))
        assertTrue(decodedLoginUrl.contains("java.webView(null,uaaBase,webAutoLoginJs(u,p,c),false)"))
        assertTrue(decodedLoginUrl.contains("function rememberCookiesFromResponse"))
        assertTrue(decodedLoginUrl.contains("function withUaaCookie"))
        assertTrue(captchaDataUriBody.contains("rememberCookiesFromResponse(r)"))
        val webAutoLoginBody = decodedLoginUrl.substringAfter("function webAutoLogin(u,p,c)")
            .substringBefore("function login()")
        val successIndex = webAutoLoginBody.indexOf("if(o&&o.ok)")
        val cloudflareIndex = webAutoLoginBody.indexOf("browserVerify(uaaBase,\"Cloudflare\")")
        assertTrue("UAA web login should handle success before opening browser fallback", successIndex >= 0)
        assertTrue("UAA web login should retain Cloudflare browser fallback", cloudflareIndex >= 0)
        assertTrue(
            "UAA web login success JSON may include cf_clearance cookie and must not trigger Cloudflare fallback",
            successIndex < cloudflareIndex
        )
        assertTrue(webAutoLoginBody.contains("var cfText=o?String(o.msg||\"\"):raw"))
        assertTrue(webAutoLoginBody.contains("isCloudflare(cfText,0,\"\")"))
        assertFalse(webAutoLoginBody.contains("isCloudflare(raw,0,\"\")"))
        assertTrue(decodedLoginUrl.contains("webAutoLogin(u,p,c)"))
        assertFalse("Toolbar login must not submit the hard-coded 404 endpoint", decodedLoginUrl.contains("uaaBase+\"/email/login\""))
        assertFalse("Toolbar login must not use guessed direct post endpoints", decodedLoginUrl.contains("java.post(a,body,headers)"))
        assertTrue(decodedLoginUrl.contains("browserVerify(uaaBase,\"Cloudflare\")"))
        assertFalse(
            "Toolbar login must not probe the page or open browser before submitting",
            decodedLoginUrl.substringAfter("function login()").contains("var h=page()")
        )
        assertFalse("UAA loginUrl must not reuse stale cached login HTML", decodedLoginUrl.contains("source.get(\"uaaLoginHtml\")"))
        assertFalse("UAA loginUrl must not call Jsoup absUrl on optional attributes", decodedLoginUrl.contains("absUrl"))
        assertTrue(decodedLoginUrl.contains("function isCloudflare"))
        assertTrue(decodedLoginUrl.contains("function isRequestFailure"))
        assertTrue(decodedLoginUrl.contains("function isCloudflareError"))
        assertTrue(decodedLoginUrl.contains("isRequestFailure(h)&&isCloudflareError(h)"))
        assertFalse("Cloudflare error matching must not run on plain response bodies", decodedLoginUrl.contains("||isCloudflareError(h)"))
        assertFalse("Cloudflare error matching must not run on post response bodies", decodedLoginUrl.contains("isCloudflareError(rb)"))
        assertFalse("Plain 403/503 errors should not be treated as Cloudflare", decodedLoginUrl.contains("Status=403|Status=503"))
        assertTrue(decodedLoginUrl.contains("java.startBrowserAwait"))
        assertTrue(decodedLoginUrl.contains("function postErrorNeedsBrowser"))
        assertTrue(decodedLoginUrl.contains("java.ajax(uaaBase)"))
        assertTrue(decodedLoginUrl.contains("cf-mitigated"))
        assertTrue(decodedLoginUrl.contains("_cf_chl_opt"))
        assertTrue(decodedLoginUrl.contains("Just a moment"))
        assertTrue(decodedLoginUrl.contains("source.putLoginHeader"))
        assertTrue(decodedLoginUrl.contains("登录失败"))
        assertFalse("UAA loginUrl must not run login while loginUi is being evaluated", decodedLoginUrl.contains("login();"))
        assertFalse("UAA source must not hard-code AI keys", uaaSource.contains("sk-"))
        assertFalse("UAA source must not hard-code external captcha services", uaaSource.contains("打码"))
    }

    @Test
    fun javdbVideoSourceIncludesLoginAndDiscoverCategories() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val javdbSource = sourceObject(sourceText, """"bookSourceUrl": "https://javdb.com/"""")
        val exploreUrl = fieldValue(javdbSource, "exploreUrl")
        val loginUrl = fieldValue(javdbSource, "loginUrl")

        assertTrue("JavDB source should exist", javdbSource.isNotBlank())
        assertTrue(loginUrl.contains("login"))
        listOf("有码", "无码", "FC2", "动漫", "排行榜").forEach {
            assertTrue("Missing JavDB primary category: $it", exploreUrl.contains(it))
        }
        listOf("censored", "uncensored", "fc2", "anime", "rankings/top").forEach {
            assertTrue("Missing JavDB primary category route: $it", exploreUrl.contains(it))
        }
        listOf("vft=4&vst=3", "vft=5&vst=3", "vft=1", "vft=2", "vft=3").forEach {
            assertTrue("Missing JavDB secondary filter: $it", exploreUrl.contains(it))
        }
        assertFalse("JavDB default categories should not include 欧美", exploreUrl.contains("western"))
    }

    @Test
    fun javdbVideoSourceUsesCurrentFilterRoutesAndExpandedTags() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val javdbSource = sourceObject(sourceText, """"bookSourceUrl": "https://javdb.com/"""")
        val exploreUrl = fieldValue(javdbSource, "exploreUrl")

        assertTrue("JavDB source should exist", javdbSource.isNotBlank())
        listOf("censored", "uncensored", "fc2", "anime").forEach {
            assertTrue("Missing JavDB primary category route: $it", exploreUrl.contains(it))
        }
        listOf("vft=0", "vft=4&vst=3", "vft=5&vst=3", "vft=1", "vft=2", "vft=3").forEach {
            assertTrue("Missing JavDB current filter route: $it", exploreUrl.contains(it))
        }
        assertTrue(exploreUrl.contains("primary.forEach"))
        assertTrue(exploreUrl.contains("filters.forEach"))
        assertTrue(exploreUrl.contains("rankTypes"))
        listOf("rankings/playback", "rankings/top").forEach {
            assertTrue("Missing JavDB ranking category: $it", exploreUrl.contains(it))
        }
        listOf("daily", "weekly", "monthly", "censored", "uncensored", "fc2", "anime").forEach {
            assertTrue("Missing JavDB ranking option: $it", exploreUrl.contains(it))
        }
        assertTrue("Missing JavDB ranking movie route", exploreUrl.contains("rankings/movies?p=${'$'}{period}&t=${'$'}{type}&page=${'$'}{page}"))
        assertFalse("JavDB discover should not include old hot tag block", exploreUrl.contains("热门 Tag"))
        assertFalse("JavDB discover should not use stale f=playable filter", exploreUrl.contains("f=playable"))
        assertFalse("JavDB discover should not use stale f=download filter", exploreUrl.contains("f=download"))
    }

    @Test
    fun javdbVideoSourceUsesSmallCoverWithoutCropDecode() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val javdbSource = sourceObject(sourceText, """"bookSourceUrl": "https://javdb.com/"""")
        val infoCoverRule = fieldValue(javdbSource, "coverUrl")
        val coverDecodeJs = fieldValue(javdbSource, "coverDecodeJs")

        assertTrue("JavDB source should exist", javdbSource.isNotBlank())
        assertTrue("JavDB should not crop covers through coverDecodeJs", coverDecodeJs.isBlank())
        assertTrue("JavDB detail should keep the small cover already captured from list/search", infoCoverRule.contains("book.coverUrl"))
        assertTrue("JavDB detail should still fall back to page thumbnails", infoCoverRule.contains(".movie-list .item img"))
        assertTrue("JavDB list/search should read thumbnail image URLs", javdbSource.contains(""""coverUrl": "img@src||img@data-src""""))
        assertFalse("JavDB small cover mode should not use BitmapFactory crop", javdbSource.contains("BitmapFactory"))
    }

    @Test
    fun javdbVideoSourceAddsClickableMagnetsToIntro() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val javdbSource = sourceObject(sourceText, """"bookSourceUrl": "https://javdb.com/"""")
        val introRule = fieldValue(javdbSource, "intro")

        assertTrue("JavDB source should exist", javdbSource.isNotBlank())
        assertTrue("JavDB intro should be generated as rich text", introRule.contains("<usehtml>"))
        assertTrue("JavDB intro should parse magnet attributes from detail HTML", introRule.contains("data-clipboard-text|href"))
        assertTrue("JavDB intro should keep magnets as vertical buttons", introRule.contains("<p><button>磁力 "))
        assertTrue("JavDB intro should not show raw magnet links", introRule.contains("<a href=\\\"").not())
        assertTrue("JavDB intro should preserve magnet URLs in button actions", introRule.contains("magnet:\\\\?xt"))
        assertTrue("JavDB intro should copy magnets when tapping a button", introRule.contains("java.copyText"))
        assertTrue("JavDB intro should open magnet apps when tapping a button", introRule.contains("java.openUrl"))
        assertTrue("JavDB intro should keep short magnet button blocks clickable without Kotlin UI changes", introRule.contains("Array(230).join('&#8203;')"))
    }

    @Test
    fun hsexVideoSourceSearchUsesPagedSearchRoute() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val hsexSource = sourceObject(sourceText, """"bookSourceUrl": "https://hsex.icu/"""")
        val searchUrl = fieldValue(hsexSource, "searchUrl")

        assertTrue("好色TV source should exist", hsexSource.isNotBlank())
        assertTrue(searchUrl.contains("search-{{page}}.htm?search={{key}}&sort=new"))
        assertFalse("好色TV search should not stay on first page", searchUrl.contains("search.htm?search={{key}}&sort=new"))
    }

    @Test
    fun xmanSourceAddsNewMangaReleaseExploreCategory() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val xmanSource = sourceObject(sourceText, """"bookSourceUrl": "https://xman7.org"""")
        val exploreUrl = fieldValue(xmanSource, "exploreUrl")
        val bookListRule = fieldValue(xmanSource, "bookList")

        assertTrue("禁漫 source should exist", xmanSource.isNotBlank())
        assertTrue("禁漫 discover should include 新漫发布 category", exploreUrl.contains("新漫发布"))
        assertTrue("禁漫 新漫发布 should point to /nm", exploreUrl.contains("https://xman7.org/nm"))
        assertTrue("禁漫 explore parser should detect the /nm page", bookListRule.contains("/nm"))
        assertTrue("禁漫 新漫发布 should parse only the second /nm section", bookListRule.contains("sections.get(1)"))
        assertFalse("禁漫 explore parser should not depend on unavailable java.net.URL", bookListRule.contains("java.net.URL"))
    }

    @Test
    fun xmanSourceCollectsEveryLazyChapterImage() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val xmanSource = sourceObject(sourceText, """"bookSourceUrl": "https://xman7.org"""")
        val contentRule = fieldValue(xmanSource, "content")

        assertTrue("Xman source should exist", xmanSource.isNotBlank())
        assertTrue("Xman content should parse the chapter document", contentRule.contains("org.jsoup.Jsoup.parse(result)"))
        assertTrue("Xman content should read lazy image URLs", contentRule.contains("data-original"))
        assertTrue("Xman content should preserve all matching images", contentRule.contains("urls.push"))
        assertTrue("Xman content should return image tags for every collected URL", contentRule.contains("urls.map(function(u)"))
        assertTrue("Xman content should include image request headers", contentRule.contains("JSON.stringify(headers)"))
        assertFalse("Xman content should not keep the old single XPath image rule", contentRule.contains("//body/div[2]/center[3]/div/img"))
    }

    @Test
    fun jmcomicSourceBuildsEveryReaderImageFromPageArr() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val jmcomicSource = sourceObject(sourceText, """"bookSourceUrl": "https://jmcomicgo.me"""")
        val contentRule = fieldValue(jmcomicSource, "content")

        assertTrue("Jmcomic source should exist", jmcomicSource.isNotBlank())
        assertTrue("Jmcomic content should read the reader page array", contentRule.contains("page_arr"))
        assertTrue("Jmcomic content should derive the CDN photo root from reader images", contentRule.contains("/media/photos/"))
        assertTrue("Jmcomic content should build image URLs from every page_arr file", contentRule.contains("root + file"))
        assertTrue("Jmcomic content should still fall back to DOM image parsing", contentRule.contains(".scramble-page img[data-original]"))
        assertTrue("Jmcomic content should include image request headers", contentRule.contains("JSON.stringify(headers)"))
        assertFalse("Jmcomic content should not keep the old album thumbnail selector", contentRule.contains("thumb-overlay-albums"))
    }

    private fun repoFile(relativePath: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull { it.exists() }
            ?: File(relativePath)
    }

    private fun sourceObject(sourceText: String, marker: String): String {
        val markerIndex = sourceText.indexOf(marker)
        if (markerIndex < 0) return ""

        val start = sourceText.lastIndexOf("\n  {", markerIndex)
        val end = sourceText.indexOf("\n  }", markerIndex)
        if (start < 0 || end < 0) return ""

        return sourceText.substring(start, end + "\n  }".length)
    }

    private fun fieldValue(sourceText: String, fieldName: String): String {
        return Regex(
            """"$fieldName"\s*:\s*"((?:\\.|[^"\\])*)",""",
            RegexOption.DOT_MATCHES_ALL
        ).find(sourceText)?.groupValues?.get(1).orEmpty()
    }

    private fun unescapeJsonString(value: String): String {
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index++]
            if (char != '\\' || index >= value.length) {
                out.append(char)
                continue
            }
            when (val escaped = value[index++]) {
                '"', '\\', '/' -> out.append(escaped)
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'u' -> {
                    val end = index + 4
                    if (end <= value.length) {
                        out.append(value.substring(index, end).toInt(16).toChar())
                        index = end
                    }
                }
                else -> out.append(escaped)
            }
        }
        return out.toString()
    }
}
