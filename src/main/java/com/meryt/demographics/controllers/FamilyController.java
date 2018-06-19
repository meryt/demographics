package com.meryt.demographics.controllers;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.FamilyResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.PersonService;

@RestController
public class FamilyController {

    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final PersonService personService;

    public FamilyController(@Autowired FamilyGenerator familyGenerator,
                            @Autowired FamilyService familyService,
                            @Autowired PersonService personService) {
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.personService = personService;
    }

    @RequestMapping("/api/families/random")
    public FamilyResponse randomFamily(@NonNull @RequestBody RandomFamilyParameters familyParameters) {
        try {
            familyParameters.validate();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        Family family = familyGenerator.generate(familyParameters);
        if (family == null) {
            return null;
        }
        Family returnedFamily = familyParameters.isPersist() ? familyService.save(family) : family;
        return new FamilyResponse(returnedFamily);
    }

    @RequestMapping("/api/families/{familyId}")
    public FamilyResponse getFamily(@PathVariable long familyId) {
        Family result = familyService.load(familyId);
        if (result == null) {
            throw new ResourceNotFoundException("No family found for ID " + familyId);
        }
        return new FamilyResponse(result);
    }

    /**
     * Create and save a family. If both partners and a wedding date are specified, a marriage will be created,
     * which includes combining households and moving in together, where applicable.
     *
     * @param familyParameters
     * @return the new family
     */
    @RequestMapping(value = "/api/families/", method = RequestMethod.POST)
    public FamilyResponse createFamily(@NonNull @RequestBody FamilyParameters familyParameters) {

        Person husband = loadPersonNullable(familyParameters.getHusbandId());
        Person wife = loadPersonNullable(familyParameters.getWifeId());
        LocalDate marriageDate = familyParameters.getWeddingDate();

        try {
            return new FamilyResponse(familyService.createAndSaveFamily(husband, wife, marriageDate));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.toString());
        }
    }

    /**
     * Loads a person if the parameter is not null.
     * @param personId the person to load, or null
     * @return a Person if the personId was not null and corresponded to a person, null if the parameter was null
     * @throws ResourceNotFoundException if the personId is non-null but does not correspond to a person
     */
    @Nullable
    private Person loadPersonNullable(Long personId) {
        if (personId == null) {
            return null;
        }

        Person person = personService.load(personId);
        if (person == null) {
            throw new ResourceNotFoundException("No person found for ID " + personId);
        }
        return person;
    }


}
