package com.ridehailing.core_api.common.util

object InputSanitizer {

  private val SCRIPT_PATTERN = Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE)
  private val HTML_TAG_PATTERN = Regex("<[^>]+>")
  private val SQL_PATTERN = Regex("(--|;|'|\")", RegexOption.IGNORE_CASE)

  fun sanitize(input: String?): String? {
    if (input == null) return null
    return input
      .replace(SCRIPT_PATTERN, "")
      .replace(HTML_TAG_PATTERN, "")
      .trim()
  }

  fun containsSuspiciousContent(input: String?): Boolean {
    if (input == null) return false
    return SCRIPT_PATTERN.containsMatchIn(input) || HTML_TAG_PATTERN.containsMatchIn(input)
  }
}
