package app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


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

    @Autowired
    ShoppingCart shoppingCart;

    @Autowired
    OrderItemRepository orderItemDb;

    @Autowired
    ClientOrderRepository clientOrderDb;

    @GetMapping("/")
    public String homePage(Model model){
        List<Product> list = productDb.findByHighestStock();
        model.addAttribute("products", list);
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
        Credentials creds = sessionCred.getCredentials();
        model.addAttribute("credentials", sessionCred.getCredentials());
        //admins can see all products while customers only see the products in stock
        if(creds != null){
            if(creds.isAdmin()){
                model.addAttribute("products", productDb.findAll());     
                return "products.html";
            }
        }
        model.addAttribute("products", productDb.findOnlyInStock()); 
        return "products.html";
    }

    @GetMapping("/details/{id}")
    public String details(@PathVariable("id") Integer id, Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());
        Optional<Product> findProduct = productDb.findById(id);   
        Product product = findProduct.get();
        model.addAttribute("product", product);
        return "details.html";
    }
    //Deny access to details page without a product id
    @GetMapping("/details")
    public void detailsNull(HttpServletResponse response){
        try {
            response.sendRedirect("/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/profile")
    public String profile(Model model){        
        model.addAttribute("credentials", sessionCred.getCredentials());
        return "profile.html";
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
            Product saveProduct = new Product(id, title, category.toLowerCase(), stock, price, description, imageHandler.saveImages(id, images));
            productDb.save(saveProduct);
        }
        try {
            response.sendRedirect("/products");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/editProduct/{id}")
    public String editProduct(@PathVariable int id, Model model){
        Optional<Product> findProduct = productDb.findById(id);   
        Product product = findProduct.get();
        model.addAttribute("product", product);
        return "edit_product.html";
    }

    @PostMapping("/confirm_product_changes")
    public @ResponseBody String completeProductChanged(@RequestBody Product product){
        Optional<Product> findProduct = productDb.findById(product.getId());
        Product productToEdit = findProduct.get();
        productToEdit.setStock(product.getStock());
        productToEdit.setPrice(product.getPrice());
        productToEdit.setDescription(product.getDescription());
        productDb.save(productToEdit);
        return "Product changes applied";
    }

    @GetMapping("/hideProduct/{id}")
    public @ResponseBody String hideProduct(@PathVariable int id){
        Optional<Product> findProduct = productDb.findById(id);
        Product product = findProduct.get();
        
        //products that have stock 0 are not displayed, hence set stock to 0 to hide
        product.setStock(0);
        productDb.save(product);
        return "Product changes applied";
    }

    @GetMapping("/sell_form")
    public String sellForm(){
        return "sell_product.html";
    }

    @GetMapping("/cart")
    public String cart(Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());
        model.addAttribute("shop_cart", shoppingCart);
        return "shop_cart.html";
    }

    @PostMapping("/addToCart")
    public @ResponseBody String addToCart(@RequestBody CartItem cartItem){
        String msg="Failed to add product to shopping cart";

        Optional<Product> findProduct = productDb.findById(cartItem.getId());
        Product product = findProduct.get();
        cartItem.setPrice(product.getPrice());
        cartItem.setTitle(product.getTitle());
        cartItem.setImage(product.getPhoto(0));
        cartItem.computeSubtotal();
        boolean result = shoppingCart.addItem(cartItem);

        if(result){
            msg="Product added successfully to the shopping cart";
        }
        return  msg;
    }

    @PostMapping("/change_quantity")
    public @ResponseBody Integer changeQuantity(@RequestBody CartItem cartItem){
        if(cartItem.getQuantity() == 0){
            return cartItem.getId();
        }
        boolean result = shoppingCart.changeQuantity(cartItem);
        if(!result){
            //if false return id to change the value to 1
           return cartItem.getId(); 
        }
        return 0;
    }

    @GetMapping("/remove_product/{id}")
    public @ResponseBody Integer removeProduct(@PathVariable int id){
        shoppingCart.removeItem(id);
        return id;
    }

    @GetMapping("/checkout")
    public String checkout(Model model){
        model.addAttribute("shop_cart", shoppingCart);
        return "checkout.html";
    }

    @PostMapping("/payment")
    public void paymentProcessing(HttpServletResponse response){
        processOrder();
        try {
            response.sendRedirect("/profile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processOrder(){
        User user = (User)sessionCred.getCredentials().getGenUser();

        //create and save the order, to be added to the orderItems
        ClientOrder order = new ClientOrder();
        order.setAddress(user.getAddress());
        order.setStatus("NEW");
        order.setTotal(shoppingCart.getTotal());
        clientOrderDb.save(order);        
        
        //add the order items to a list and save them all at once
        List<OrderItem> tempListOrderItems = new ArrayList<OrderItem>();

        //list of ids to get the products of based on all the ids ==>
        List<Integer> productsId = new ArrayList<Integer>();
        for(CartItem cItem : shoppingCart.getCartItems()){
            productsId.add(cItem.getId());
        }
        List<Product> requiredProducts = productDb.findAllById(productsId);
        
        // ==> then populate a hash map based on id and product for quick access
        HashMap<Integer, Product> requiredProductsMap = new HashMap<>();
        for(Product pd : requiredProducts){
            requiredProductsMap.put(pd.getId(), pd);
        }

        //iterate to create each order item, add item to product and save
        for(CartItem item : shoppingCart.getCartItems()){
            //constructor takes quantity, price, subtotal or quantity, price, subtotal, product, order
            Product product = requiredProductsMap.get(item.getId());
            OrderItem orderItem = new OrderItem(item.getQuantity(), product.getPrice(), item.getSubtotal(), product, order);
            tempListOrderItems.add(orderItem);
            product.getOrderItems().add(orderItem);
            productDb.save(product);           
        }     

        //save the order items to db
        orderItemDb.saveAll(tempListOrderItems);
        //generate order number (random number + order id)
        String orderNumber = generateOrderNumber(order.getId());
        //set and save
        order.setOrderNumber(orderNumber);        
        orderItemDb.saveAll(tempListOrderItems);
        order.setOrderItems(tempListOrderItems);
        order.setUser(user);
        clientOrderDb.save(order);
        user.getOrders().add(order);
        userDb.save(user);
        //clear the shopping cart
        shoppingCart.setCartItems();
    } 

    private String generateOrderNumber(long id){
        Random rand = new Random();
        int number = rand.nextInt(10000) + 1000;
        return number + "" + id;
    }

    @GetMapping("/viewOrders")
    public String orderView(Model model){
        model.addAttribute("credentials", sessionCred.getCredentials());

        if(sessionCred.getCredentials() != null){
            if(sessionCred.getCredentials().isAdmin()){
                model.addAttribute("orders", clientOrderDb.findAll());
                return "order_view_admin.html";
            }
        }
        return "order_view.html";
    }

    @GetMapping("/change_order_status/{id}/{status}")
    public @ResponseBody String changeOrderStatus(@PathVariable long id, @PathVariable String status){
        Optional<ClientOrder> findOrder = clientOrderDb.findById(id);

        if(findOrder.isPresent()){            
            ClientOrder order = findOrder.get();
            order.setStatus(status);
            clientOrderDb.save(order);
            return "&diams; Order status changed successfully &diams;";
        }
        else{
            return "&diams; Order status did not change &diams;";
        }            
    }
}