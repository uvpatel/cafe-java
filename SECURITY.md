# Security Guidelines

This document provides security best practices and requirements for the Café POS Backend project.

## Table of Contents

- [Security Principles](#security-principles)
- [Authentication & Authorization](#authentication--authorization)
- [Data Protection](#data-protection)
- [Input Validation](#input-validation)
- [Output Encoding](#output-encoding)
- [API Security](#api-security)
- [Database Security](#database-security)
- [Dependency Management](#dependency-management)
- [Secrets Management](#secrets-management)
- [Logging & Monitoring](#logging--monitoring)
- [Incident Response](#incident-response)
- [Security Testing](#security-testing)

---

## Security Principles

### Defense in Depth

Use multiple layers of security:

```
Application → Authentication → Authorization → Validation → Database
  Layer 1        Layer 2          Layer 3        Layer 4     Layer 5
```

### Least Privilege

Each user/service has minimum permissions needed:

```java
// ✅ Good – Specific role
@PreAuthorize("hasRole('MANAGER')")
public void closeSession() { ... }

// ❌ Bad – Overly broad
@PreAuthorize("isAuthenticated()")
public void closeSession() { ... }
```

### Fail Securely

On error, deny access by default:

```java
// ✅ Good – Deny if unknown state
if (order.getStatus() != CONFIRMED) {
    throw new OrderCannotBeCanceledException();
}

// ❌ Bad – Allow on unknown state
if (order.getStatus() == CONFIRMED) {
    throw new OrderCannotBeCanceledException();
}
```

### Security by Design

Build security in from the start:

- ✅ All tables include `organization_id` for multi-tenancy
- ✅ All updates track `modified_by` and `modified_at`
- ✅ All deletions use soft-delete (`deleted_at`)
- ✅ All payments use optimistic locking to prevent double-charging

---

## Authentication & Authorization

### JWT Token Structure

```
Header:
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "2024-08"
}

Payload:
{
  "sub": "user-id",
  "email": "user@cafe.com",
  "org_id": "org-uuid",
  "outlet_id": "outlet-uuid",
  "roles": ["CASHIER", "WAITER"],
  "permissions": ["order.create", "order.read", "order.update"],
  "iat": 1629859200,
  "exp": 1629945600
}

Signature: HMACSHA256(header + payload, secret)
```

### Token Lifecycle

```java
@Service
public class AuthenticationService {
    
    // Tokens are short-lived (1 hour)
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);
    
    // Refresh tokens are longer-lived (7 days)
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    
    public TokenResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException());
        
        // 1. Verify password with Argon2id
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        
        // 2. Check if account is active
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        
        // 3. Generate short-lived access token
        var accessToken = tokenService.generateToken(user, TOKEN_VALIDITY);
        
        // 4. Generate long-lived refresh token
        var refreshToken = tokenService.generateToken(user, REFRESH_TOKEN_VALIDITY);
        
        // 5. Store refresh token in secure cookie (httpOnly, secure)
        var response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setExpiresIn(TOKEN_VALIDITY.getSeconds());
        response.setTokenType("Bearer");
        
        return response;
    }
    
    public void logout(String token) {
        // Add token to blacklist (Redis with TTL = token expiry)
        tokenBlacklist.add(token, TOKEN_VALIDITY);
    }
}
```

### Authorization Patterns

**Role-Based Access Control (RBAC):**
```java
@Service
public class OrderApplicationService {
    
    @PreAuthorize("hasRole('CASHIER') or hasRole('MANAGER')")
    public OrderResponse createOrder(CreateOrderDto dto) {
        // Only cashiers and managers can create orders
        return createOrderInternal(dto);
    }
    
    @PreAuthorize("hasRole('MANAGER')")
    public void cancelOrder(OrderId id) {
        // Only managers can cancel orders
        var order = repository.findById(id).orElseThrow();
        order.cancel();
        repository.save(order);
    }
    
    @PreAuthorize("@securityService.isOutletManager(#outletId)")
    public void closeSession(OutletId outletId) {
        // Only outlet manager for this specific outlet
        sessionService.close(outletId);
    }
}
```

**Permission-Based Access Control (PBAC):**
```java
@Component
public class SecurityService {
    
    public boolean hasPermission(String permission) {
        var claims = SecurityContext.getClaims();
        var permissions = claims.get("permissions", List.class);
        return permissions.contains(permission);
    }
    
    public void checkOrderAccess(OrderId orderId) {
        var order = orderRepository.findById(orderId).orElseThrow();
        var userOrganization = SecurityContext.getOrganizationId();
        
        if (!order.getOrganizationId().equals(userOrganization)) {
            throw new AccessDeniedException("Order not found");
        }
    }
}
```

---

## Data Protection

### Encryption at Rest

**Database passwords and secrets:**
```yaml
# application-prod.yml – Use environment variables
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        
  redis:
    password: ${REDIS_PASSWORD}
```

**Sensitive fields in database:**
```java
@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    private UUID id;
    
    private String email;
    
    // ✅ Password stored as hash, never plaintext
    @Column(name = "password_hash")
    private String passwordHash;  // Argon2id hash
    
    // ✅ Sensitive data encrypted
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;
    
    // ✅ API keys encrypted
    @Convert(converter = EncryptedStringConverter.class)
    private String apiKey;
}

// Custom converter for encryption
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    private final EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptionService.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptionService.decrypt(dbData);
    }
}
```

### Encryption in Transit

**Force HTTPS:**
```yaml
server:
  servlet:
    session:
      cookie:
        secure: true        # HTTPS only
        http-only: true     # No JavaScript access
        same-site: STRICT   # CSRF protection
        
security:
  require-https: true
```

**HSTS Header:**
```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers()
            .addHeaderWriter(new StaticHeadersWriter(
                "Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload"
            ));
        
        return http.build();
    }
}
```

### Password Hashing

**Use Argon2id (OWASP standard):**
```java
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
            16,              // saltLength
            32,              // hashLength
            2,               // parallelism
            65536,           // memory (KB)
            3                // iterations
        );
    }
}

// Usage
@Service
public class UserService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public void setPassword(User user, String plainPassword) {
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
    }
    
    public boolean verifyPassword(User user, String plainPassword) {
        return passwordEncoder.matches(plainPassword, user.getPasswordHash());
    }
}
```

### Multi-Tenancy Data Isolation

**Enforce at every level:**
```java
@Service
@Transactional
public class OrderApplicationService {
    
    public OrderResponse createOrder(CreateOrderDto dto) {
        // 1. Get tenant from security context
        var organizationId = SecurityContext.getOrganizationId();
        
        // 2. Create order with tenant key
        var order = Order.create(organizationId, dto.getLines());
        
        // 3. Persist (JPA cascade handles relationships)
        repository.save(order);
        
        return OrderResponse.fromOrder(order);
    }
    
    public Optional<OrderResponse> getOrder(OrderId id) {
        var organizationId = SecurityContext.getOrganizationId();
        
        // ✅ Repository enforces tenant isolation
        return repository.findById(id, organizationId)
            .map(OrderResponse::from);
    }
}

// Repository enforces tenant filtering
@Repository
public class JpaOrderRepository {
    
    public Optional<Order> findById(OrderId id, OrganizationId orgId) {
        return springRepository.findByIdAndOrganizationId(
            id.value(),
            orgId.value()
        );
    }
}

// Database also enforces with constraints
@RepositoryDefinition(domainClass = OrderEntity.class, idClass = UUID.class)
interface SpringOrderRepository extends JpaRepository<OrderEntity, UUID> {
    
    Optional<OrderEntity> findByIdAndOrganizationId(UUID id, UUID orgId);
}
```

---

## Input Validation

### Validation at API Boundary

**Use Jakarta Bean Validation:**
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        // @Valid annotation triggers validation before method executes
        // Validation errors return 422 Unprocessable Entity
        
        var dto = CreateOrderDto.from(request);
        var response = service.createOrder(dto);
        return ResponseEntity.status(CREATED).body(response);
    }
}

@Data
public class CreateOrderRequest {
    
    @NotNull(message = "Order items required")
    @NotEmpty(message = "At least one item required")
    @Size(min = 1, max = 100, message = "Order items must be 1-100")
    private List<OrderLineRequest> items;
}

@Data
public class OrderLineRequest {
    
    @NotNull(message = "Product ID required")
    private UUID productId;
    
    @NotNull(message = "Quantity required")
    @Positive(message = "Quantity must be positive")
    @Max(value = 999, message = "Quantity exceeds maximum")
    private Integer quantity;
}
```

### Validation in Domain

**Business rule validation:**
```java
public class Order {
    
    public Order(List<OrderLine> lines) {
        // ✅ Validate business rules
        if (lines.isEmpty()) {
            throw new OrderMustHaveLinesException();
        }
        
        var totalQuantity = lines.stream()
            .map(OrderLine::getQuantity)
            .reduce(0, Integer::sum);
        
        if (totalQuantity > MAX_ITEMS_PER_ORDER) {
            throw new OrderExceedsMaxItemsException(totalQuantity);
        }
        
        // Calculate and validate total
        var total = calculateTotal(lines);
        if (total.isGreaterThan(MAX_ORDER_TOTAL)) {
            throw new OrderTotalExceededException(total);
        }
        
        this.lines = lines;
        this.total = total;
    }
}
```

### Sanitization

**Prevent injection attacks:**
```java
public class FormulaSanitizer {
    
    // Prevent CSV injection via formula prefix
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // Check for formula prefixes
        char firstChar = value.charAt(0);
        if (firstChar == '=' || firstChar == '+' || 
            firstChar == '-' || firstChar == '@' || 
            firstChar == '\t' || firstChar == '\r') {
            
            // Prepend single quote to escape
            return "'" + value;
        }
        
        return value;
    }
}

// Usage in reporting
var sanitizedName = FormulaSanitizer.sanitize(product.getName());
writer.println(sanitizedName + "," + quantity + "," + revenue);
```

---

## Output Encoding

### JSON Response Security

**Use Object Mapper with security features:**
```java
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        
        // Prevent XXE attacks
        mapper.disable(DeserializationFeature.ACCEPT_EXTERNAL_DTD);
        mapper.disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);
        
        // Don't include null values in responses
        mapper.setSerializationInclusion(Include.NON_NULL);
        
        // Use ISO-8601 date format
        mapper.registerModule(new JavaTimeModule());
        
        return mapper;
    }
}
```

**Avoid exposing sensitive information:**
```java
@Data
public class OrderResponse {
    
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    
    // ✅ Don't expose internal fields
    @JsonIgnore
    private BigDecimal calculatedTax;  // Hidden from response
    
    // ✅ Use @JsonProperty for renaming
    @JsonProperty("total_amount")
    private Money total;
    
    // ✅ Don't expose user passwords or tokens
    // @JsonIgnore
    // private String userPassword;
}
```

---

## API Security

### CORS Configuration

**Restrict cross-origin requests:**
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList(
            "https://app.cafe.example.com",
            "https://admin.cafe.example.com"
        ));
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### Rate Limiting

**Prevent brute-force attacks:**
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RateLimitStore store;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain chain) 
            throws ServletException, IOException {
        
        var clientId = getClientIdentifier(request);
        var key = "rate_limit:" + clientId;
        
        var currentCount = store.increment(key, 1);
        if (currentCount == 1) {
            store.expire(key, 60);  // 1 minute window
        }
        
        if (currentCount > 100) {  // 100 requests per minute
            response.setStatus(TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return;
        }
        
        response.addHeader("X-RateLimit-Limit", "100");
        response.addHeader("X-RateLimit-Remaining", 
                          String.valueOf(100 - currentCount));
        
        chain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Use user ID if authenticated, else IP
        var user = SecurityContext.getUser();
        if (user != null) {
            return "user:" + user.getId();
        }
        
        return "ip:" + getClientIp(request);
    }
}
```

---

## Database Security

### Row-Level Security

**PostgreSQL RLS ensures tenant isolation:**
```sql
-- Enable RLS on orders table
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their organization's orders
CREATE POLICY orders_isolation ON orders
    USING (organization_id = current_setting('app.current_org_id')::uuid);

-- Apply policy to operations
CREATE POLICY orders_insert ON orders
    FOR INSERT
    WITH CHECK (organization_id = current_setting('app.current_org_id')::uuid);

-- Set context before queries
SET app.current_org_id = 'org-uuid-here';
SELECT * FROM orders;  -- Only returns org's orders
```

### Database Backups

**Encrypted backups with retention policy:**
```bash
#!/bin/bash
# Backup with encryption (Neon PostgreSQL)
pg_dump "$DATABASE_URL" | \
  openssl enc -aes-256-cbc -out cafepos_$(date +%Y%m%d).sql.enc

# Retention: Keep last 30 days
find . -name "cafepos_*.sql.enc" -mtime +30 -delete

# Store in S3 with encryption
aws s3 cp cafepos_$(date +%Y%m%d).sql.enc \
  s3://backups-prod/cafepos/ \
  --sse AES256 \
  --storage-class GLACIER
```

---

## Dependency Management

### Vulnerability Scanning

**Regular dependency audits:**
```bash
# Check for known vulnerabilities
./mvnw org.owasp:dependency-check-maven:check

# Update dependencies
./mvnw versions:update-properties

# Scan with Trivy (container images)
trivy image cafe-pos-backend:latest
```

**Dependencies to be cautious with:**
- Avoid `commons-collections` (known RCE vulnerabilities)
- Use `jackson-databind` latest version
- Keep Spring Security updated (patches often released)
- Regular updates for database drivers

---

## Secrets Management

### Environment-Based Configuration

**Never commit secrets:**
```yaml
# ✅ application-prod.yml (environment variables)
spring:
  datasource:
    url: ${DATABASE_URL}
    password: ${DATABASE_PASSWORD}
    
  security:
    oauth2:
      resourceserver:
        jwt:
          key-value: ${JWT_SECRET_KEY}
          
  redis:
    password: ${REDIS_PASSWORD}
    ssl: true
```

**Set in deployment:**
```bash
# Kubernetes
kubectl create secret generic cafe-pos-secrets \
  --from-literal=database-password=xxx \
  --from-literal=jwt-secret=yyy

# Docker Compose (development only)
export DATABASE_PASSWORD=secure-pass
docker compose up
```

---

## Logging & Monitoring

### Security Event Logging

**Log sensitive operations:**
```java
@Component
public class SecurityAuditLogger {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);
    
    @EventListener(AuthenticationSuccessEvent.class)
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("Authentication successful: user={}, ip={}, timestamp={}",
            event.getAuthentication().getName(),
            getClientIp(),
            Instant.now());
    }
    
    @EventListener(AuthenticationFailureEvent.class)
    public void onAuthenticationFailure(AuthenticationFailureEvent event) {
        log.warn("Authentication failed: user={}, reason={}, ip={}, timestamp={}",
            event.getAuthentication().getName(),
            event.getException().getMessage(),
            getClientIp(),
            Instant.now());
    }
    
    @EventListener(OrderCreatedEvent.class)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order created: order_id={}, user={}, org_id={}, amount={}, timestamp={}",
            event.getOrderId(),
            SecurityContext.getUserId(),
            SecurityContext.getOrganizationId(),
            event.getOrderTotal(),
            Instant.now());
    }
}
```

### Log Masking

**Never log sensitive data:**
```java
@Component
public class LogMaskingConverter implements ClassicConverter {
    
    @Override
    public String convert(ILoggingEvent event) {
        var message = event.getFormattedMessage();
        
        // Mask credit card numbers
        message = message.replaceAll(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b",
            "****-****-****-****");
        
        // Mask tokens
        message = message.replaceAll(
            "Bearer [\\w.-]+",
            "Bearer ***");
        
        // Mask passwords
        message = message.replaceAll(
            "password[\"']?\\s*[:=][\"']?[^\"'\\s]+",
            "password=***");
        
        return message;
    }
}
```

---

## Incident Response

### Security Incident Process

1. **Detect** – Monitoring alerts, user reports
2. **Respond** – Isolate affected systems, stop attack
3. **Investigate** – Analyze logs, identify cause
4. **Fix** – Apply patch, remediate vulnerability
5. **Communicate** – Notify affected users if needed
6. **Review** – Post-incident analysis, prevent recurrence

### Emergency Response

**For suspected breach:**
```bash
# 1. Rotate compromised credentials
kubectl set env deployment/api \
  DATABASE_PASSWORD=new-password \
  JWT_SECRET=new-secret

# 2. Review logs
kubectl logs -f deployment/api | grep -i "error\|failed\|unauthorized"

# 3. Inspect database
psql -c "SELECT * FROM activity_log WHERE created_at > now() - interval '1 hour'"

# 4. Notify team
# Send incident notification to security@cafepos.example.com

# 5. Document incident
# Create GitHub issue with:
# - What happened
# - When discovered
# - Impact assessment
# - Actions taken
# - Follow-up items
```

---

## Security Testing

### OWASP Top 10

Test for common vulnerabilities:

| Risk | Testing | Mitigation |
|------|---------|-----------|
| **Injection** | SQL, command injection tests | Parameterized queries |
| **Broken Auth** | Auth bypass, token attacks | Strong auth, token validation |
| **Sensitive Data** | Encryption at rest/transit | TLS, field encryption |
| **XML/XXE** | DTD processing | Disable external DTD |
| **Broken Access** | Unauthorized access tests | RLS, multi-tenancy checks |
| **Security Misc** | Security headers, CSP | Header configuration |
| **XSS** | Input validation tests | Output encoding |
| **CSRF** | CSRF token validation | SameSite cookies |
| **Using Components** | Dependency scanning | Regular updates |
| **Insufficient Logging** | Audit trail verification | Comprehensive logging |

### Penetration Testing Checklist

```
□ Authentication
  □ Brute force login attempts
  □ Session hijacking
  □ Token expiration
  □ Password reset flow

□ Authorization
  □ Privilege escalation
  □ Cross-organization access
  □ Rate limiting bypass
  □ API endpoint restrictions

□ Input Validation
  □ SQL injection
  □ Command injection
  □ XXE attacks
  □ Large payload attacks

□ Data Protection
  □ HTTPS enforcement
  □ Encryption validation
  □ Sensitive data in logs
  □ Backup encryption

□ API Security
  □ CORS misconfiguration
  □ Missing security headers
  □ Verbose error messages
  □ Directory traversal
```

---

## Security Contacts

**Report Security Vulnerabilities:**
- **Email:** security@cafepos.example.com
- **PGP Key:** Available on request
- **Response Time:** Within 24 hours

**Do NOT:**
- ❌ Open public GitHub issues for vulnerabilities
- ❌ Post on social media
- ❌ Notify other users before we've patched

---

## Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/sql-syntax.html)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CWE Top 25](https://cwe.mitre.org/top25/archive/2023/2023.html)

---

**Last Updated:** August 2026  
**Author:** Security Team  
**Review Cycle:** Quarterly
