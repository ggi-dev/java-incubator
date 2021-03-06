package boost.brain.course.projects.controller;

import boost.brain.course.common.projects.ProjectDto;
import boost.brain.course.common.projects.ProjectStatus;
import boost.brain.course.projects.Constants;
import boost.brain.course.projects.controller.exceptions.BadRequestException;
import boost.brain.course.projects.controller.exceptions.NotFoundException;
import boost.brain.course.projects.model.Project;
import boost.brain.course.projects.model.ProjectMapper;
import boost.brain.course.projects.repository.ProjectRepository;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Log
@RestController
@RequestMapping(Constants.PROJECTS_CONTROLLER_PREFIX)
public class ProjectController implements ProjectControllerSwaggerAnnotations {

    private ProjectRepository projectRepository;
    private ProjectMapper projectMapper;
    EmailValidator emailValidator = new EmailValidator();

    @Autowired
    public ProjectController(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    @ResponseBody
    @PostMapping(Constants.CREATE_PREFIX)
    public ProjectDto create(@RequestBody ProjectDto projectDto) {
        // Input validation
        if (StringUtils.isEmpty(projectDto.getProjectName()) ||
                StringUtils.isEmpty(projectDto.getDescription()) ||
                StringUtils.isEmpty(projectDto.getAuthor()) ||
                StringUtils.isEmpty(projectDto.getProjectUrl())) {
            throw new BadRequestException();
        }
        //Setting default values
        projectDto.setStatus(ProjectStatus.PREPARATION);

        //Creating a new project in the database
        ProjectDto result = projectMapper.toProjectDto(projectRepository.save(projectMapper.toProject(projectDto)));
        if (result == null) {
            throw new BadRequestException();
        }
        return result;
    }

    @Override
    @ResponseBody
    @GetMapping(Constants.READ_PREFIX + "/{id}")
    public ProjectDto read(@PathVariable int id) {
        if (id < 1) {
            throw new BadRequestException();
        }
        log.info("inside findById(projectId) method; id: " + id);
        ProjectDto result = projectMapper.toProjectDto(projectRepository.findByProjectId(id));
        if (result == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }
        log.info("found project: " + result.toString());
        return result;
    }

    @Override
    @ResponseBody
    @GetMapping(Constants.COUNT_PREFIX)
    public int count(){
        int result = projectRepository.countAllBy();
        log.info("method: count(); count: "+ result);
        return result;
    }

    @Override
    @ResponseBody
    @DeleteMapping(Constants.DELETE_PREFIX + "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String delete(@PathVariable int id) {
        log.info("deleting project with id: "+ id);
        if (id < 1) {
            throw new BadRequestException();
        }
        if (projectRepository.deleteProjectByProjectId(id) == 1) {
            return HttpStatus.OK.getReasonPhrase();
        } else {
            log.severe("not found project");
            throw new NotFoundException();
        }
    }

    @Override
    @ResponseBody
    @GetMapping(Constants.PAGE_PREFIX + "/{page}/{size}")
    public Page<ProjectDto> page(
            @PathVariable int page,
            @PathVariable int size) {
        if((page < 0) || (size < 1)) {
            throw new BadRequestException();
        }
        Pageable firstPageWithTwoElements = PageRequest.of(page,size);
        Page<ProjectDto> result = projectMapper.toProjectDtoPage(projectRepository.findAll(firstPageWithTwoElements));
        if (result == null) {
            throw  new NotFoundException();
        }
        return result;
    }

    @Override
    @ResponseBody
    @PatchMapping(Constants.UPDATE_PREFIX)
    @ResponseStatus(HttpStatus.OK)
    public String update(@RequestBody ProjectDto projectDto) {
        log.info("method: update");
        // Input validation
        if (projectDto.getProjectId() < 1 ||
                StringUtils.isEmpty(projectDto.getProjectName()) ||
                StringUtils.isEmpty(projectDto.getDescription()) ||
                StringUtils.isEmpty(projectDto.getProjectUrl()) ||
                StringUtils.isEmpty(projectDto.getAuthor()) ||
                projectDto.getStatus() == null) {
            throw new BadRequestException();
        }
        // Updating the project in the database
        if (projectRepository.update(
                projectDto.getProjectUrl(),
                projectDto.getDescription(),
                projectDto.getProjectName(),
                projectDto.getStatus(),
                projectDto.getAuthor(),
                projectDto.getProjectId()) == 1) {
            log.info("Success");
        } else {
            throw new NotFoundException();
        }

        return HttpStatus.OK.getReasonPhrase();
    }

    @Override
    @ResponseBody
    @PostMapping(Constants.FOR_IDS_PREFIX)
    public List<ProjectDto> forIds(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException();
        }
        List<ProjectDto> result =  projectMapper.toProjectDtos(projectRepository.findAllByProjectIdIn(ids));
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    @Override
    @ResponseBody
    @GetMapping(Constants.ALL_PREFIX)
    public List<ProjectDto> all() {
        List<ProjectDto> result =  projectMapper.toProjectDtos(projectRepository.findAll());
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    @Override
    @ResponseBody
    @GetMapping(Constants.IF_EXISTS_PREFIX + "/{id}")
    public boolean ifExists(@PathVariable int id) {
        if (id < 1) {
            throw new BadRequestException();
        }
        return projectRepository.existsByProjectId(id);
    }

    @Override
    @ResponseBody
    @PatchMapping(Constants.UPDATE_PREFIX + Constants.STATUS_PREFIX + "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String updateStatus(@RequestBody ProjectStatus status, @PathVariable int id) {
        // Input validation
        if (id < 1 || status == null) {
            throw new BadRequestException();
        }

        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            throw new NotFoundException();
        }

        // Project status update
        project.setStatus(status);

        // Saving the updated project to the database
        if (projectRepository.save(project) == null) {
            throw new BadRequestException();
        }

        return HttpStatus.OK.getReasonPhrase();
    }

    @Override
    @ResponseBody
    @GetMapping("/{id}" + Constants.PARTICIPATING_USERS + Constants.CREATE_PREFIX + "/{email}")
    @ResponseStatus(HttpStatus.OK)
    public String createParticipatingUser(@PathVariable String email, @PathVariable int id) {
        log.info("create participating user: " + email);
        // Input validation
        if (id < 1 || StringUtils.isEmpty(email) || !this.checkEmail(email)) {
            throw new BadRequestException();
        }
        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }
        //Moving a user from waiting users to participating users
        project.getParticipatingUsers().add(email);
        project.getWaitingUsers().remove(email);
        projectRepository.save(project);

        return HttpStatus.OK.getReasonPhrase();
    }

    @Override
    @ResponseBody
    @GetMapping("/{id}" + Constants.PARTICIPATING_USERS + Constants.ALL_PREFIX)
    public Set<String> allParticipatingUsers(@PathVariable int id) {
        log.info("all participating users for: " + id);
        // Input validation
        if (id < 1) {
            throw new BadRequestException();
        }
        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }

        return project.getParticipatingUsers();
    }

    @Override
    @ResponseBody
    @DeleteMapping("/{id}" + Constants.PARTICIPATING_USERS + Constants.DELETE_PREFIX + "/{email}")
    @ResponseStatus(HttpStatus.OK)
    public String deleteParticipatingUser(@PathVariable String email, @PathVariable int id) {
        log.info("delete participating user: " + email);
        // Input validation
        if (id < 1 || StringUtils.isEmpty(email) || !this.checkEmail(email)) {
            throw new BadRequestException();
        }
        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }
        if (!project.getParticipatingUsers().contains(email)) {
            log.severe("User is not found");
            throw new NotFoundException();
        }
        //Deleting a user from participating users
        project.getParticipatingUsers().remove(email);
        projectRepository.save(project);

        return HttpStatus.OK.getReasonPhrase();
    }

    @Override
    @ResponseBody
    @GetMapping("/{id}" + Constants.WAITING_USERS + Constants.CREATE_PREFIX + "/{email}")
    @ResponseStatus(HttpStatus.OK)
    public String createWaitingUser(@PathVariable String email, @PathVariable int id) {
        log.info("create waiting user: " + email);
        // Input validation
        if (id < 1 || StringUtils.isEmpty(email) || !this.checkEmail(email)) {
            throw new BadRequestException();
        }
        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }
        if (project.getParticipatingUsers().contains(email)) {
            log.severe("User is in participating users!");
            throw new BadRequestException();
        }
        //Creating a user to waiting users
        project.getWaitingUsers().add(email);
        projectRepository.save(project);

        return HttpStatus.OK.getReasonPhrase();
    }

    @Override
    @ResponseBody
    @GetMapping("/{id}" + Constants.WAITING_USERS + Constants.ALL_PREFIX)
    public Set<String> allWaitingUsers(@PathVariable int id) {
        log.info("all waiting users for: " + id);
        // Input validation
        if (id < 1) {
            throw new BadRequestException();
        }
        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }

        return project.getWaitingUsers();
    }

    @Override
    @ResponseBody
    @DeleteMapping("/{id}" + Constants.WAITING_USERS + Constants.DELETE_PREFIX + "/{email}")
    @ResponseStatus(HttpStatus.OK)
    public String deleteWaitingUser(@PathVariable String email, @PathVariable int id) {
        log.info("delete waiting user: " + email);
        // Input validation
        if (id < 1 || StringUtils.isEmpty(email) || !this.checkEmail(email)) {
            throw new BadRequestException();
        }
        // Getting a project by ID
        Project project = projectRepository.findByProjectId(id);
        if (project == null) {
            log.severe("not found project");
            throw new NotFoundException();
        }
        if (!project.getWaitingUsers().contains(email)) {
            log.severe("User is not found");
            throw new NotFoundException();
        }
        //Deleting a user from waiting users
        project.getWaitingUsers().remove(email);
        projectRepository.save(project);

        return HttpStatus.OK.getReasonPhrase();
    }

    private boolean checkEmail(final String email) {
        if (!emailValidator.isValid(email, null)) {
            log.severe("Email is not valid!");
            return false;
        }
        return true;
    }
}
