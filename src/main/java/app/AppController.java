package app;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AppController {
        
    @Autowired
    AdminRepository adminDb;

    @Autowired
    SessionHandler sessionCred;

    @Autowired
    UserRepository userDb;

    @Autowired
    PasswordHandler passwordHandler;

    @Autowired
    ImageHandler imageHandler;

    @Autowired
    ProductRepository productDb;

    @GetMapping("/")
    public String homePage(Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());
        return "home.html";
    }

    @GetMapping("/login_signup")
    public String login_signupPage(HttpServletResponse response){    
        if(sessionCred.getCredentials() != null){
            try {
                response.sendRedirect("/");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
        return "login_signup.html";
    }

    @PostMapping("/login")
    public void loginHandler(String email, String password, HttpServletResponse response){
        Admin admin = null;
        admin = adminDb.findAdminByEmailAndPassword(email, passwordHandler.hashPass(password));
        User user = null;
        user = userDb.findUserByEmailAndPassword(email, passwordHandler.hashPass(password));
        if(admin != null || user != null){            
            if(admin != null){            
                sessionCred.setCredentials(new Credentials(admin, true));
            }
            else{
                sessionCred.setCredentials(new Credentials(user));
            }            
        
            try {
                response.sendRedirect("/");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                response.sendRedirect("/login_signup?re=logfail&msg=Failed+to+log+in.+Wrong+email+or+password.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping("/signup")
    public void signupHandler(String firstname, String surname, String address,
                                String email, String password, String password_confirm, HttpServletResponse response){
        User user = null;
        user = userDb.findUserByEmail(email);        
        String msg = "";
        if(user == null){
            User saveUser = new User(firstname, surname, address, email, passwordHandler.hashPass(password)); 
            userDb.save(saveUser);           
            msg="Registered+Successfully";
            try {
                response.sendRedirect("/login_signup?re=success&msg="+msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            if(!password.equals(password_confirm)){
                msg="Passwords+do+not+match";
            }
            else{
                msg="User+already+exits";
            }

            try {
                response.sendRedirect("/login_signup?re=signfail&msg="+msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } 
    
    @GetMapping("/logout")
    public void logout(HttpServletResponse response){
        sessionCred.setCredentials(null);
        try {
            response.sendRedirect("/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @GetMapping("/products")
    public String products(Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());
        return "products.html";
    }

    @GetMapping("/details")
    public String details(Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());
        return "details.html";
    }

    @GetMapping("/profile")
    public String profile(Model model){        
        model.addAttribute("credentials", sessionCred.getCredentials());
        String page = "profile_restrict.html";

        if(sessionCred.getCredentials() == null){
            return page;
        }
        else if(sessionCred.getCredentials().isAdmin()){
            page = "profile_admin.html";
        }
        else{
            page = "profile_user.html";
        }
        return page;
    }

    @GetMapping("/userdetails")
    public String returnUserDetails(Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());
        if(sessionCred.getCredentials().isAdmin()){
            return "admin_details.html";
        }
        return "user_details.html";
    }
   
    @PostMapping("/sell_product")
    public void sellProduct(int id, String title, String category, int stock, double price,
                            String description, @RequestParam(name = "images") MultipartFile[] images,
                            HttpServletResponse response){
        Optional<Product> findProduct = productDb.findById(id);         

        if(findProduct.isPresent()){
            Product saveProduct = findProduct.get();
            saveProduct.setStock(saveProduct.getStock() + stock);
            productDb.save(saveProduct);
        }
        else{
            Product saveProduct = new Product(id, title, category, stock, price, description, imageHandler.saveImages(title, id, images));
            productDb.save(saveProduct);
        }
        try {
            response.sendRedirect("/products");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/sell_form")
    public String sellForm(){
        return "sell_product.html";
    }
}