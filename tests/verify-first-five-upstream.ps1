$ErrorActionPreference = 'Stop'

function Assert-FileContains {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Pattern,
        [Parameter(Mandatory = $true)][string]$Message
    )

    $text = Get-Content -Raw -LiteralPath $Path
    if ($text -notmatch $Pattern) {
        throw "$Message ($Path)"
    }
}

Assert-FileContains 'app/src/main/java/io/legado/app/lib/cronet/CronetHelper.kt' 'customIp\.remove\(url\)\s*\?:\s*customIp\.remove\(url\.substringBefore\(' 'Cronet custom IP lookup must ignore query when needed'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeByJSoup.kt' 'lastIndex\s*==\s*-1[\s\S]*"text"' 'CSS rule without @last must default to text'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeByJSoup.kt' 'val\s+cleanElements\s*=\s*elements\.clone\(\)' 'HTML extraction must not mutate reused JSoup elements'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeByJSoup.kt' 'rules\.getOrNull\(1\)' 'Indexed JSoup helper rules must tolerate missing arguments'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt' 'data\s+class\s+ResolvedSourceRule' 'AnalyzeRule must resolve source rules without mutating cached SourceRule'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt' 'makeUpRule\(result:\s*Any\?\):\s*ResolvedSourceRule' 'SourceRule.makeUpRule must return an immutable resolved rule'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt' 'ruleParts\.sortedBy\s*\{\s*it\.start\s*\}' 'JS and WebJS source rules must keep source order'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt' 'val\s+pageIndex\s*=\s*page\.coerceAtLeast\(1\)' 'AnalyzeUrl page placeholder must clamp low page values'
Assert-FileContains 'app/src/main/java/io/legado/app/model/analyzeRule/RuleAnalyzer.kt' 'if\s*\(pos\s*>=\s*queue\.length\)\s*return' 'RuleAnalyzer.trim must handle empty and blank rules'

Assert-FileContains 'app/src/main/java/io/legado/app/help/config/CoverCollectionManager.kt' 'File\(Uri\.parse\(value\)\.path\.orEmpty\(\)\)\.isFile' 'Mixed cover mode must validate file:// cover paths'
Assert-FileContains 'app/src/main/java/io/legado/app/help/config/CoverCollectionManager.kt' 'File\(value\)\.isAbsolute\s*->\s*File\(value\)\.isFile' 'Mixed cover mode must validate absolute file cover paths'
Assert-FileContains 'app/src/main/java/io/legado/app/ui/widget/image/CoverImageView.kt' 'drawNameOverlayForCurrentCover' 'CoverImageView must track overlay state per loaded cover'
Assert-FileContains 'app/src/main/java/io/legado/app/ui/widget/image/CoverImageView.kt' 'allowNameOverlay:\s*Boolean\?\s*=\s*null' 'CoverImageView.load must accept explicit overlay policy'

Assert-FileContains 'app/src/main/java/io/legado/app/ui/book/manga/ReadMangaActivity.kt' 'setResult\(RESULT_OK\)\s*[\r\n]+\s*super\.finish\(\)' 'Manga add-to-shelf confirmation must finish after OK'
Assert-FileContains 'app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt' 'setResult\(RESULT_OK\)\s*[\r\n]+\s*callBackBookEnd\(\)\s*[\r\n]+\s*super\.finish\(\)' 'Book add-to-shelf confirmation must callback and finish after OK'

Assert-FileContains 'app/src/main/java/io/legado/app/help/TTS.kt' 'initGeneration' 'Inline TTS helper must ignore stale init callbacks'
Assert-FileContains 'app/src/main/java/io/legado/app/service/TTSReadAloudService.kt' 'activeUtteranceId' 'System TTS service must ignore stale utterance callbacks'
Assert-FileContains 'app/src/main/java/io/legado/app/service/TTSReadAloudService.kt' 'retryParagraphKey' 'System TTS service must retry current paragraph before skipping'
Assert-FileContains 'app/src/main/java/io/legado/app/model/ReadAloud.kt' 'commandClass\(\)' 'ReadAloud commands must target the currently running service class'
Assert-FileContains 'app/src/main/java/io/legado/app/service/BaseReadAloudService.kt' 'runningClass:\s*Class<\*>\?' 'Base read aloud service must expose the currently running class'
Assert-FileContains 'app/src/main/java/io/legado/app/service/BaseReadAloudService.kt' 'private\s+fun\s+acquireWakeLocks\(\)' 'Wake locks must be acquired idempotently'
Assert-FileContains 'app/src/main/java/io/legado/app/service/BaseReadAloudService.kt' 'timeMinute\s*<=\s*0' 'Read aloud timer must not start a countdown for non-positive values'
Assert-FileContains 'app/src/main/java/io/legado/app/receiver/MediaButtonReceiver.kt' 'ReadAloud\.prevChapter\(context\)' 'Media previous key must control the running read-aloud service when active'
Assert-FileContains 'app/src/main/java/io/legado/app/receiver/MediaButtonReceiver.kt' 'ReadAloud\.nextChapter\(context\)' 'Media next key must control the running read-aloud service when active'
Assert-FileContains 'app/src/main/java/io/legado/app/service/HttpReadAloudService.kt' 'httpTtsSnapshot' 'HTTP TTS service must use a stable TTS source snapshot while playing'
Assert-FileContains 'app/src/main/java/io/legado/app/service/HttpReadAloudService.kt' 'cancelHttpWork\(\)' 'HTTP TTS service must cancel download and request jobs together'

Write-Host 'first-five upstream verification passed'
