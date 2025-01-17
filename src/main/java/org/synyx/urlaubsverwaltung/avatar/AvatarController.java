package org.synyx.urlaubsverwaltung.avatar;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class AvatarController {

    private static final String UNKNOWN_PERSON = "?";
    private final SvgService svgService;

    AvatarController(final SvgService svgService) {
        this.svgService = svgService;
    }

    @GetMapping(value = "/web/avatar", produces = "image/svg+xml")
    @ResponseBody
    public ResponseEntity<String> avatar(@RequestParam(value = "name", defaultValue = UNKNOWN_PERSON) String name, Locale locale) {

        final Map<String, Object> model = Map.of("initials", getInitials(name.trim()));
        final String svg = svgService.createSvg("svg/avatar", locale, model);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .contentType(MediaType.valueOf("image/svg+xml"))
            .body(svg);
    }

    private static String getInitials(String niceName) {

        final int idxLastWhitespace = niceName.lastIndexOf(' ');
        if (idxLastWhitespace == -1) {
            return niceName.substring(0, 1).toUpperCase();
        }

        return (niceName.charAt(0) + niceName.substring(idxLastWhitespace + 1, idxLastWhitespace + 2)).toUpperCase();
    }
}
