package com.hiresense.api.resume.parsing;

import java.io.InputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

    public String extract(InputStream content) {
        try {
            var handler = new BodyContentHandler(-1);
            var metadata = new Metadata();
            var parser = new AutoDetectParser();
            parser.parse(content, handler, metadata);
            return handler.toString().trim();
        } catch (Exception e) {
            throw new TextExtractionException("Unable to extract text from document", e);
        }
    }
}
