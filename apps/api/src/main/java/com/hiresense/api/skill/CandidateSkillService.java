package com.hiresense.api.skill;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateSkillService {

    private final CandidateSkillRepository candidateSkillRepository;
    private final com.hiresense.api.user.UserRepository userRepository;

    public CandidateSkillService(
            CandidateSkillRepository candidateSkillRepository, com.hiresense.api.user.UserRepository userRepository) {
        this.candidateSkillRepository = candidateSkillRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void attachSkills(Long userId, List<Skill> skills, SkillSource source) {
        if (skills.isEmpty()) {
            return;
        }
        var user = userRepository.getReferenceById(userId);
        for (Skill skill : skills) {
            if (candidateSkillRepository
                    .findByUserIdAndSkillId(userId, skill.getId())
                    .isEmpty()) {
                candidateSkillRepository.save(new CandidateSkill(user, skill, source));
            }
        }
    }
}
