package org.piramalswasthya.cho.fhir_utils

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.model.ResourceType

private const val patientUrl : String = "http://hl7.org/fhir/StructureDefinition/Patient#Patient"

private const val appointmentUrl : String = "http://hl7.org/fhir/StructureDefinition/Appointment#Appointment"

class FhirExtension constructor(resourceType : ResourceType){

    private var baseUrl : String = when(resourceType){
        ResourceType.Patient -> patientUrl
        ResourceType.Appointment -> appointmentUrl
        else -> {
            ""
        }
    };

    fun getExtenstion(url: String, value: Type) : Extension {
        val extension = Extension();
        extension.url = url
        extension.setValue(value)
        return extension
    }

    fun getUrl(variableName: String) : String {
        var url = baseUrl;
        url += ".$variableName";
        return url
    }

    fun getStringType(value : String) : StringType {
        val str = StringType(value);
        return str;
    }

    fun getCoding(code : String, display: String) : Coding {
        val cdt = Coding();
        cdt.code = code
        cdt.display = display
        return cdt;
    }

}