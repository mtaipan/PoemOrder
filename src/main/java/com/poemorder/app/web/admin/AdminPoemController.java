package com.poemorder.app.web.admin;

import com.poemorder.app.domain.poem.Poem;
import com.poemorder.app.domain.poem.PoemStatus;
import com.poemorder.app.service.PoemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/poems")
public class AdminPoemController {

    private final PoemService poemService;

    public AdminPoemController(PoemService poemService) {
        this.poemService = poemService;
    }

    private static final int PAGE_SIZE = 20;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("poems", poemService.list(
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt"))));
        return "admin/poems-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Poem poem = new Poem();
        poem.setStatus(PoemStatus.DRAFT);
        model.addAttribute("poem", poem);
        model.addAttribute("statuses", PoemStatus.values());
        model.addAttribute("mode", "create");
        return "admin/poem-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("poem") Poem poem,
                         BindingResult br,
                         Model model) {
        if (br.hasErrors()) {
            model.addAttribute("statuses", PoemStatus.values());
            model.addAttribute("mode", "create");
            return "admin/poem-form";
        }
        Poem saved = poemService.create(poem);
        return "redirect:/admin/poems/" + saved.getId() + "/edit?ok";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("poem", poemService.getOrThrow(id));
        model.addAttribute("statuses", PoemStatus.values());
        model.addAttribute("mode", "edit");
        return "admin/poem-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("poem") Poem poem,
                       BindingResult br,
                       Model model) {
        if (br.hasErrors()) {
            model.addAttribute("statuses", PoemStatus.values());
            model.addAttribute("mode", "edit");
            return "admin/poem-form";
        }
        poemService.update(id, poem);
        return "redirect:/admin/poems/" + id + "/edit?ok";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        poemService.delete(id);
        return "redirect:/admin/poems?deleted";
    }
}
