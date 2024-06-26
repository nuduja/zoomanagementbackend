package com.fulltack.zooManagment.service;

import com.fulltack.zooManagment.Requests.AnimalRequest;
import com.fulltack.zooManagment.exception.AnimalNotFoundException;
import com.fulltack.zooManagment.exception.ServiceException;
import com.fulltack.zooManagment.generators.PDFGeneratorService;
import com.fulltack.zooManagment.model.Animal;
import com.fulltack.zooManagment.repository.AnimalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnimalService {

    @Autowired
    private AnimalRepository repository;

    @Autowired
    private PDFGeneratorService pdfGeneratorService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Animal convertToAnimal(AnimalRequest animalRequest) {
        Animal animal = new Animal();
        animal.setAnimalId(UUID.randomUUID().toString().split("-")[0]);
        animal.setAnimalSpeciesId(animalRequest.getAnimalSpeciesId());
        animal.setAnimalSpeciesName(animalRequest.getAnimalSpeciesName());
        animal.setName(animalRequest.getName());
        animal.setEnclosureId(animalRequest.getEnclosureId());
        animal.setBirthDate(animalRequest.getBirthDate());
        animal.setBirthCountry(animalRequest.getBirthCountry());
        animal.setDescription(animalRequest.getDescription());
        return animal;
    }

    public List<Animal> getAllAnimals() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            throw new ServiceException("Error Occurred while fetching all Animals", e);
        }
    }

    public Animal getAnimalByAnimalId(String animalId) {
        try {
            Animal animal = repository.findByAnimalId(animalId);

            if (animal == null) {
                throw new AnimalNotFoundException("Animal with ID " + animalId + " not found");
            }

            return animal;
        } catch (Exception e) {
            throw new ServiceException("Error occurred while fetching specific animal", e);
        }
    }

    public List<Animal> getAnimalsBySpeciesId(String animalSpeciesId) {
        try {
            return repository.findByAnimalSpeciesId(animalSpeciesId);
        } catch (Exception e) {
            throw new ServiceException("Error Occurred while fetching all Animals", e);
        }
    }

    public String addAnimal(AnimalRequest animalRequest) {
        try {
            Animal animal = convertToAnimal(animalRequest);
            if (animal.getAnimalId() == null || animal.getName() == null) {
                throw new IllegalArgumentException("Animal type and status must be valid.");
            }
            repository.save(animal);
            return "Animal Created Successful";
        } catch (Exception e) {
            throw new ServiceException("Error occurred while adding an animal", e);
        }
    }

    public String deleteAnimalByAnimalId(String animalId) {
        try {
            if (repository.existsByAnimalId(animalId)) {
                repository.deleteByAnimalId(animalId);
                return "Animal Deleted Successfully";
            } else {
                return "Animal Does not exists";
            }
        } catch (Exception e) {
            throw new ServiceException("Error Occurred while Deleting Animal", e);
        }
    }

    public List<Animal> searchAnimals(String animalId, String animalSpeciesId, String name) {
        try {
            Query query = new Query();
            List<Criteria> criteria = new ArrayList<>();

            if (animalId != null && !animalId.isEmpty()) {
                criteria.add(Criteria.where("animalId").regex(animalId, "i"));
            }
            if (animalSpeciesId != null && !animalSpeciesId.isEmpty()) {
                criteria.add(Criteria.where("animalSpeciesId").regex(animalSpeciesId, "i"));
            }
            if (name != null && !name.isEmpty()) {
                criteria.add(Criteria.where("name").regex(name, "i"));
            }

            if (!criteria.isEmpty()) {
                query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
            }

            return mongoTemplate.find(query, Animal.class);
        } catch (Exception e) {
            throw new ServiceException("Error Searching Animal", e);
        }
    }

    public void updateAnimalByAnimalId(String animalId, Map<String, Object> updates) {
        Query query = new Query(Criteria.where("animalId").is(animalId));
        Update update = new Update();
        updates.forEach((key, value) -> {
            if (!key.equals("id") && !key.equals("animalId")) {
                update.set(key, value);
            }
        });
        mongoTemplate.findAndModify(query, update, Animal.class);
    }

    public ByteArrayInputStream generateAnimalsPDF() {
        List<Animal> animals = repository.findAll();
        return pdfGeneratorService.animalReport(animals);
    }
}