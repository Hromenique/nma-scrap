package br.com.hrom.nmascrap.scraper

data class CourseReference(val name: String, val details: String, val lessons: List<LessonReference>)