package com.smart.controller;

import java.io.File;
import java.lang.StackWalker.Option;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entites.Contact;
import com.smart.entites.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
   
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	//methos for adding common data to response 
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		String name = principal.getName();
		System.out.println("USERNAME :"+name);
		
		User userByUserName = userRepository.getUserByUserName(name);
		
		System.out.println("USER :"+userByUserName);
		
		model.addAttribute("user", userByUserName);
	} 
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal) {
		model.addAttribute("title", "user dashboard");
	return "normal/user_dashboard";
	}
	
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage")MultipartFile file, Principal principal,HttpSession session) {
		
		try {
			
		String name = principal.getName();
		
		User user = userRepository.getUserByUserName(name);
		
		
		 
		
		//processing and uploading file...
		
		if(file.isEmpty()) {
			
			//if the file is empty then try out message
			
			System.out.println("File is empety");
			contact.setImages("contact.png");
		
		}else {
			//file the file to folder and update the name to contact
			contact.setImages(file.getOriginalFilename());
			
			File file2 = new ClassPathResource("static/img").getFile();
			
			Path path = Paths.get(file2.getAbsolutePath()+File.separator+file.getOriginalFilename());
			
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			
			System.out.println("Image is Uploaded");
		}
		
		
		
		
		
		user.getContacts().add(contact);
		contact.setUser(user);
		
		this.userRepository.save(user);
		
		System.out.println("DATA "+contact);
		
		System.out.println("Added to data base");
		
		//message success...
		session.setAttribute("message", new Message("Your Contact is added !! Add more...", "success"));
		
		}catch (Exception e) {
			
			System.out.println(e.getMessage());
			e.printStackTrace();
			
			//message error..
			
			session.setAttribute("message", new Message("Some thing went wrong try again!!", "danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	//show contacts handler
	//per page =5[n]
	//Current page =0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model model,Principal principal) {
		model.addAttribute("title","Show User Contacts");
		
		//contact ki list 
		   String name = principal.getName();
		   User user = this.userRepository.getUserByUserName(name);
		   
		   Pageable pageable = PageRequest.of(page, 3);
		   
		   
		   Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		  
		   model.addAttribute("contacts", contacts);
		   
		   model.addAttribute("currentPage",page);
		   
		   model.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing particular contact details.
	@RequestMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		System.out.println(cId);
		
		Optional<Contact> findById = this.contactRepository.findById(cId);
		Contact contact = findById.get();
		
		String name = principal.getName();
		User user= userRepository.getUserByUserName(name);
	  
		if(user.getId()==contact.getUser().getId()) {
		   model.addAttribute("contact", contact);
		   model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid")Integer cId,Model model,HttpSession httpSession,Principal principal) {
		
		Contact contact  = this.contactRepository.findById(cId).get();
		 
		User user = this.userRepository.getUserByUserName(principal.getName());
		
		user.getContacts().remove(contact);
		
		this.contactRepository.delete(contact);
		
		this.userRepository.save(user);
		
		httpSession.setAttribute("message", new Message("Contact deleted succesfully.....", "success"));
		
		//Check...
		
		return "redirect:/user/show-contacts/0";
	}
	
	
	//open update from handler
	@PostMapping("update-contact/{cid}")
	public String updateForm(@PathVariable("cid")Integer cid,Model model) {
		
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		model.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
	//update contact handler 
	
	@RequestMapping(value = "/process-update",method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model model,HttpSession session,Principal principal) {
		
		try {
			
			Contact oldcontact= this.contactRepository.findById(contact.getcId()).get();
			
			//image....
			if(!file.isEmpty()) {
				//file work..
				//rewrite..
				
				//delete old photo
				
				//update new photo
				File deletefile = new ClassPathResource("static/img").getFile();
				File file3 = new File(deletefile,oldcontact.getImages());
				file3.delete();
				
				
				
				File file2 = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(file2.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImages(file.getOriginalFilename());
				
			}else {
				contact.setImages(oldcontact.getImages());
				
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("meaasge", new Message("Your contact is updated", "success"));
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("CONTACT NAME "+contact.getName());
		System.out.println("CONTACT ID "+contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile page");
		return "normal/profile";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
