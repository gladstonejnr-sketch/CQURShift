package secureshift.service;

import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Site;

import java.util.List;

/**
 * Service class for Site business logic.
 * Delegates persistence to SiteRepositoryJDBC.
 */
public class SiteService {

    private final SiteRepositoryJDBC siteRepository;

    // ✅ Constructor
    public SiteService(SiteRepositoryJDBC siteRepository) {
        this.siteRepository = siteRepository;
    }

    // ✅ Get all sites
    public List<Site> getAllSites() {
        return siteRepository.loadAllSites();
    }

    // ✅ Get site by ID using efficient database lookup
    public Site getSiteById(int id) {
        return siteRepository.findById(id)
                .orElse(null);
    }

    // ✅ Get site by name using efficient database lookup
    public Site getSiteByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Site name cannot be empty");
        }
        return siteRepository.findByName(name)
                .orElse(null);
    }

    // ✅ Add a new site
    public void addSite(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Site cannot be null");
        }
        if (site.getName() == null || site.getName().isBlank()) {
            throw new IllegalArgumentException("Site name cannot be empty");
        }

        // ✅ Check for duplicate site name
        boolean nameExists = siteRepository.findByName(site.getName()).isPresent();
        if (nameExists) {
            throw new IllegalArgumentException(
                    "Site with this name already exists: " + site.getName());
        }

        siteRepository.saveSite(site);
        System.out.println("✅ Site added: " + site.getName());
    }

    // ✅ Update an existing site
    public void updateSite(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Site cannot be null");
        }
        if (site.getName() == null || site.getName().isBlank()) {
            throw new IllegalArgumentException("Site name cannot be empty");
        }

        // ✅ Check site exists before updating
        if (!siteRepository.exists(site.getId())) {
            throw new IllegalArgumentException("Site not found: " + site.getId());
        }

        siteRepository.updateSite(site);
        System.out.println("✅ Site updated: " + site.getName());
    }

    // ✅ Delete a site by ID
    public void deleteSite(int siteId) {
        // ✅ Check site exists before deleting
        if (!siteRepository.exists(siteId)) {
            throw new IllegalArgumentException("Site not found: " + siteId);
        }

        siteRepository.deleteSite(siteId);
        System.out.println("✅ Site deleted: " + siteId);
    }

    // ✅ Check if a site exists
    public boolean siteExists(int siteId) {
        return siteRepository.exists(siteId);
    }

    // ✅ Get sites by region
    public List<Site> getSitesByRegion(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region cannot be empty");
        }
        return siteRepository.loadAllSites()
                .stream()
                .filter(s -> s.getRegion() != null &&
                        s.getRegion().equalsIgnoreCase(region))
                .toList();
    }

    // ✅ Get sites requiring a specific skill
    public List<Site> getSitesByRequiredSkill(String skill) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("Skill cannot be empty");
        }
        return siteRepository.loadAllSites()
                .stream()
                .filter(s -> s.getRequiredSkills() != null &&
                        s.getRequiredSkills().contains(skill))
                .toList();
    }

    // ✅ Get all available regions
    public List<String> getAllRegions() {
        return siteRepository.loadAllRegions();
    }

    // ✅ Get all required skills across all sites
    public List<String> getAllSkills() {
        return siteRepository.loadAllSkills();
    }

    // ✅ Get total number of sites using efficient database count
    public int getTotalSiteCount() {
        return siteRepository.count();
    }
}

