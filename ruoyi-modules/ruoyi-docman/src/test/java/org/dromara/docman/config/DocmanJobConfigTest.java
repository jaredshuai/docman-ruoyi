package org.dromara.docman.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocmanJobConfigTest {

    @Test
    void shouldHaveDefaultValues() {
        DocmanJobConfig config = new DocmanJobConfig();

        assertEquals(3, config.getDocumentReminderPendingDays());
        assertEquals(3, config.getReminderAdvanceDays());
        assertEquals(5, config.getMaxReminderCount());
    }

    @Test
    void shouldSetAndGetDocumentReminderPendingDays() {
        DocmanJobConfig config = new DocmanJobConfig();

        config.setDocumentReminderPendingDays(7);

        assertEquals(7, config.getDocumentReminderPendingDays());
    }

    @Test
    void shouldSetAndGetReminderAdvanceDays() {
        DocmanJobConfig config = new DocmanJobConfig();

        config.setReminderAdvanceDays(5);

        assertEquals(5, config.getReminderAdvanceDays());
    }

    @Test
    void shouldSetAndGetMaxReminderCount() {
        DocmanJobConfig config = new DocmanJobConfig();

        config.setMaxReminderCount(10);

        assertEquals(10, config.getMaxReminderCount());
    }
}