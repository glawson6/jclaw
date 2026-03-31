package io.jaiclaw.examples.voicecall;

import io.jaiclaw.core.tool.ToolCallback;
import io.jaiclaw.core.tool.ToolContext;
import io.jaiclaw.core.tool.ToolDefinition;
import io.jaiclaw.core.tool.ToolProfile;
import io.jaiclaw.core.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Wires demo-specific tools that the agent uses during voice calls.
 * These simulate a backend system with appointments and customer data.
 */
@Configuration
public class VoiceCallDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(VoiceCallDemoConfig.class);

    @Bean
    ToolCallback lookupAppointmentsTool() {
        return new LookupAppointmentsTool();
    }

    @Bean
    ToolCallback rescheduleAppointmentTool() {
        return new RescheduleAppointmentTool();
    }

    @Bean
    ToolCallback lookupCustomerTool() {
        return new LookupCustomerTool();
    }

    // --- Tool implementations ---

    /**
     * Looks up upcoming appointments for a phone number.
     * In production this would query a real scheduling system.
     */
    static class LookupAppointmentsTool implements ToolCallback {

        private static final String INPUT_SCHEMA = """
                {
                  "type": "object",
                  "properties": {
                    "phoneNumber": {
                      "type": "string",
                      "description": "Customer phone number in E.164 format"
                    }
                  },
                  "required": ["phoneNumber"]
                }""";

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(
                    "lookup_appointments",
                    "Look up upcoming appointments for a customer by phone number. "
                            + "Returns appointment details including date, time, provider, and type.",
                    "appointments",
                    INPUT_SCHEMA,
                    Set.of(ToolProfile.FULL, ToolProfile.MINIMAL)
            );
        }

        @Override
        public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
            String phone = (String) parameters.get("phoneNumber");
            log.info("Looking up appointments for {}", phone);

            // Simulated appointment data
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            return new ToolResult.Success("""
                    {
                      "customer": "Jane Smith",
                      "phone": "%s",
                      "appointments": [
                        {
                          "id": "APT-1042",
                          "date": "%s",
                          "time": "14:30",
                          "provider": "Dr. Rodriguez",
                          "type": "Annual Checkup",
                          "location": "Main Street Clinic, Suite 200",
                          "notes": "Bring insurance card and photo ID"
                        }
                      ]
                    }""".formatted(phone, tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
    }

    /**
     * Reschedules an appointment to a new date/time.
     */
    static class RescheduleAppointmentTool implements ToolCallback {

        private static final String INPUT_SCHEMA = """
                {
                  "type": "object",
                  "properties": {
                    "appointmentId": {
                      "type": "string",
                      "description": "Appointment ID to reschedule"
                    },
                    "newDate": {
                      "type": "string",
                      "description": "New date in YYYY-MM-DD format"
                    },
                    "newTime": {
                      "type": "string",
                      "description": "New time in HH:MM format"
                    }
                  },
                  "required": ["appointmentId", "newDate", "newTime"]
                }""";

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(
                    "reschedule_appointment",
                    "Reschedule an existing appointment to a new date and time. "
                            + "Returns confirmation with the updated appointment details.",
                    "appointments",
                    INPUT_SCHEMA,
                    Set.of(ToolProfile.FULL, ToolProfile.MINIMAL)
            );
        }

        @Override
        public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
            String appointmentId = (String) parameters.get("appointmentId");
            String newDate = (String) parameters.get("newDate");
            String newTime = (String) parameters.get("newTime");
            log.info("Rescheduling {} to {} at {}", appointmentId, newDate, newTime);

            return new ToolResult.Success("""
                    {
                      "status": "confirmed",
                      "appointmentId": "%s",
                      "newDate": "%s",
                      "newTime": "%s",
                      "provider": "Dr. Rodriguez",
                      "message": "Appointment rescheduled successfully"
                    }""".formatted(appointmentId, newDate, newTime));
        }
    }

    /**
     * Looks up customer information by phone number.
     */
    static class LookupCustomerTool implements ToolCallback {

        private static final String INPUT_SCHEMA = """
                {
                  "type": "object",
                  "properties": {
                    "phoneNumber": {
                      "type": "string",
                      "description": "Customer phone number in E.164 format"
                    }
                  },
                  "required": ["phoneNumber"]
                }""";

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(
                    "lookup_customer",
                    "Look up customer information by phone number. "
                            + "Returns name, account status, and preferences.",
                    "customers",
                    INPUT_SCHEMA,
                    Set.of(ToolProfile.FULL, ToolProfile.MINIMAL)
            );
        }

        @Override
        public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
            String phone = (String) parameters.get("phoneNumber");
            log.info("Looking up customer for {}", phone);

            return new ToolResult.Success("""
                    {
                      "name": "Jane Smith",
                      "phone": "%s",
                      "accountStatus": "active",
                      "memberSince": "2021-03-15",
                      "preferredLanguage": "en",
                      "preferredContactMethod": "phone",
                      "notes": "Prefers afternoon appointments"
                    }""".formatted(phone));
        }
    }
}
