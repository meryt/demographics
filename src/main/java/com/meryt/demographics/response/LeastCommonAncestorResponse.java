package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.service.PersonService;

@Getter
public class LeastCommonAncestorResponse {

    private final String relationship;
    private final LeastCommonAncestorResponseTree leastCommonAncestor;

     /**
     * @param person the person of interest
     * @param otherPerson the related person
     * @param lcaRelationship the least common ancestor relationship between these two people
     * @param relationshipDescription the calculated relations between the person of interest and the related person.
     *                                The relationship will state how otherPerson is related to person, e.g.
     *                                "otherPerson is the mother of person"
     * @param personService the person service, used to load the tree of related persons (is not saved in the object
     *                      but only used in the constructor)
     */
    public LeastCommonAncestorResponse(@NonNull Person person,
                                       @NonNull Person otherPerson,
                                       @NonNull LeastCommonAncestorRelationship lcaRelationship,
                                       @NonNull Relationship relationshipDescription,
                                       @NonNull PersonService personService) {
        this.relationship = String.format("%d %s is the %s of %d %s",
                otherPerson.getId(), otherPerson.getName(), relationshipDescription.getName(), person.getId(),
                person.getName());

        this.leastCommonAncestor = new LeastCommonAncestorResponseTree(person, otherPerson, lcaRelationship, personService);
    }
}
