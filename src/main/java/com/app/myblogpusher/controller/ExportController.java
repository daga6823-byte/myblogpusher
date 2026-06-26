package com.app.myblogpusher.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.MenuMaster;
import com.app.myblogpusher.entity.TypoCorrection;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.ArticleCategoryRepository;
import com.app.myblogpusher.repository.ArticleWorkRepository;
import com.app.myblogpusher.repository.MenuMasterRepository;
import com.app.myblogpusher.repository.TypoCorrectionRepository;
import com.app.myblogpusher.repository.UserMasterRepository;

@Controller
public class ExportController {

    @Autowired private UserMasterRepository userMasterRepository;
    @Autowired private ArticleCategoryRepository articleCategoryRepository;
    @Autowired private ArticleWorkRepository articleWorkRepository;
    @Autowired private TypoCorrectionRepository typoCorrectionRepository;
    @Autowired private MenuMasterRepository menuMasterRepository;

    @GetMapping("/export/all")
    public ResponseEntity<byte[]> exportAll() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        writeCsv(zos, "user_master.csv",
            new String[]{"user_id","login_id","password","user_name","email","reg_ip","reg_port","reg_hostname","user_agent","role","status","create_date","update_date","create_user","update_user"},
            userMasterRepository.findAll(),
            (UserMaster u) -> new String[]{
                str(u.getUserId()), u.getLoginId(), u.getPassword(), u.getUserName(), u.getEmail(),
                u.getRegIp(), str(u.getRegPort()), u.getRegHostname(), u.getUserAgent(),
                str(u.getRole()), u.getStatus(), str(u.getCreateDate()), str(u.getUpdateDate()),
                str(u.getCreateUser()), str(u.getUpdateUser())
            });

        writeCsv(zos, "article_category.csv",
            new String[]{"category_id","user_id","category_name","create_date","update_date","create_user","update_user"},
            articleCategoryRepository.findAll(),
            (ArticleCategory c) -> new String[]{
                str(c.getCategoryId()), str(c.getUserId()), c.getCategoryName(),
                str(c.getCreateDate()), str(c.getUpdateDate()), str(c.getCreateUser()), str(c.getUpdateUser())
            });

        writeCsv(zos, "article_work.csv",
            new String[]{"work_id","user_id","category_id","title","content","create_date","update_date","create_user","update_user"},
            articleWorkRepository.findAll(),
            (ArticleWork w) -> new String[]{
                str(w.getWorkId()), str(w.getUserId()), str(w.getCategoryId()), w.getTitle(), w.getContent(),
                str(w.getCreateDate()), str(w.getUpdateDate()), str(w.getCreateUser()), str(w.getUpdateUser())
            });

        writeCsv(zos, "typo_correction.csv",
            new String[]{"typo_id","category_id","wrong_word","correct_word","create_date","update_date","create_user","update_user"},
            typoCorrectionRepository.findAll(),
            (TypoCorrection t) -> new String[]{
                str(t.getTypoId()), str(t.getCategoryId()), t.getWrongWord(), t.getCorrectWord(),
                str(t.getCreateDate()), str(t.getUpdateDate()), str(t.getCreateUser()), str(t.getUpdateUser())
            });

        writeCsv(zos, "menu_master.csv",
            new String[]{"menu_url","menu_name","min_role","create_date","update_date","create_user","update_user"},
            menuMasterRepository.findAll(),
            (MenuMaster m) -> new String[]{
                m.getMenuUrl(), m.getMenuName(), str(m.getMinRole()),
                str(m.getCreateDate()), str(m.getUpdateDate()), str(m.getCreateUser()), str(m.getUpdateUser())
            });

        zos.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=myblogpusher_export.zip");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
    }

    private <T> void writeCsv(ZipOutputStream zos, String fileName, String[] header,
                               List<T> rows, RowMapper<T> mapper) throws Exception {

        ByteArrayOutputStream rowBaos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(rowBaos);

        writer.println(String.join(",", header));

        for (T row : rows) {
            String[] values = mapper.map(row);
            String[] escaped = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                escaped[i] = escapeCsv(values[i]);
            }
            writer.println(String.join(",", escaped));
        }

        writer.flush();

        zos.putNextEntry(new ZipEntry(fileName));
        zos.write(rowBaos.toByteArray());
        zos.closeEntry();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        String[] map(T row);
    }
}