package com.app.myblogpusher.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.EnglishDictionary;

public interface EnglishDictionaryRepository extends JpaRepository<EnglishDictionary, Long> {
	Optional<EnglishDictionary> findByJapaneseWord(String japaneseWord);
}