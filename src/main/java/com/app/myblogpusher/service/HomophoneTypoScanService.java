package com.app.myblogpusher.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class HomophoneTypoScanService {

    // {文中に出た語, 混同されやすい語, 読み}
    private static final List<String[]> HOMOPHONE_PAIRS = List.of(
        new String[]{"以外",   "意外",   "いがい"},
        new String[]{"意外",   "以外",   "いがい"},
        new String[]{"以降",   "移行",   "いこう"},
        new String[]{"移行",   "以降",   "いこう"},
        new String[]{"意思",   "意志",   "いし"},
        new String[]{"意志",   "意思",   "いし"},
        new String[]{"以前",   "依然",   "いぜん"},
        new String[]{"依然",   "以前",   "いぜん"},
        new String[]{"異常",   "以上",   "いじょう"},
        new String[]{"以上",   "異常",   "いじょう"},
        new String[]{"機械",   "機会",   "きかい"},
        new String[]{"機会",   "機械",   "きかい"},
        new String[]{"感心",   "関心",   "かんしん"},
        new String[]{"関心",   "感心",   "かんしん"},
        new String[]{"確率",   "確立",   "かくりつ"},
        new String[]{"確立",   "確率",   "かくりつ"},
        new String[]{"過程",   "課程",   "かてい"},
        new String[]{"課程",   "過程",   "かてい"},
        new String[]{"解放",   "開放",   "かいほう"},
        new String[]{"開放",   "解放",   "かいほう"},
        new String[]{"公開",   "後悔",   "こうかい"},
        new String[]{"後悔",   "公開",   "こうかい"},
        new String[]{"紹介",   "照会",   "しょうかい"},
        new String[]{"照会",   "紹介",   "しょうかい"},
        new String[]{"支持",   "指示",   "しじ"},
        new String[]{"指示",   "支持",   "しじ"},
        new String[]{"思考",   "試行",   "しこう"},
        new String[]{"試行",   "思考",   "しこう"},
        new String[]{"志向",   "思考",   "しこう"},
        new String[]{"習得",   "修得",   "しゅうとく"},
        new String[]{"修得",   "習得",   "しゅうとく"},
        new String[]{"収集",   "収拾",   "しゅうしゅう"},
        new String[]{"収拾",   "収集",   "しゅうしゅう"},
        new String[]{"制作",   "製作",   "せいさく"},
        new String[]{"製作",   "制作",   "せいさく"},
        new String[]{"体制",   "態勢",   "たいせい"},
        new String[]{"態勢",   "体制",   "たいせい"},
        new String[]{"対象",   "対照",   "たいしょう"},
        new String[]{"対照",   "対象",   "たいしょう"},
        new String[]{"追求",   "追及",   "ついきゅう"},
        new String[]{"追及",   "追究",   "ついきゅう"},
        new String[]{"追究",   "追求",   "ついきゅう"},
        new String[]{"補償",   "保証",   "ほしょう"},
        new String[]{"保証",   "保障",   "ほしょう"},
        new String[]{"保障",   "補償",   "ほしょう"},
        new String[]{"変換",   "返還",   "へんかん"},
        new String[]{"返還",   "変換",   "へんかん"},
        new String[]{"普及",   "不急",   "ふきゅう"},
        new String[]{"不急",   "普及",   "ふきゅう"}
    );

    public List<LanguageToolService.LanguageToolMatch> scan(String content) {
        List<LanguageToolService.LanguageToolMatch> results = new ArrayList<>();

        for (String[] pair : HOMOPHONE_PAIRS) {
            String found      = pair[0];
            String suggestion = pair[1];
            String reading    = pair[2];

            if (!content.contains(found)) continue;

            // 同一wrongWordの重複登録を防ぐ
            boolean alreadyAdded = results.stream()
                    .anyMatch(m -> m.getMatchedText().equals(found));
            if (alreadyAdded) continue;

            int fromPos = content.indexOf(found);
            results.add(new LanguageToolService.LanguageToolMatch(
                    fromPos,
                    fromPos + found.length(),
                    found,
                    suggestion,
                    "同音異義語の可能性があります（読み：" + reading + "）",
                    "Misspelling"
            ));
        }
        return results;
    }
}