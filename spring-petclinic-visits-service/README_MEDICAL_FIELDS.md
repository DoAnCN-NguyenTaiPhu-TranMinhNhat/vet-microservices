# Medical Data Fields - Visits Service

## Tổng quan

Visits Service đã được mở rộng để hỗ trợ các trường dữ liệu y tế cho hệ thống chẩn đoán bệnh thú cưng.

## Các trường dữ liệu y tế mới

### 1. `temperature` (Nhiệt độ)
- **Type**: `BigDecimal`
- **Database**: `NUMERIC(4,1)`
- **Range**: 35.0 - 43.0°C
- **Validation**: @DecimalMin(35.0), @DecimalMax(43.0)
- **Required**: Optional

### 2. `weight_kg` (Trọng lượng)
- **Type**: `BigDecimal`
- **Database**: `NUMERIC(5,2)`
- **Range**: 0.1 - 100.0 kg
- **Validation**: @DecimalMin(0.1), @DecimalMax(100.0)
- **Required**: Optional

### 3. `symptoms_list` (Danh sách triệu chứng)
- **Type**: `String`
- **Database**: `VARCHAR(5000)`
- **Max Length**: 5000 characters
- **Validation**: @Size(max = 5000)
- **Required**: Optional
- **Example**: "fever, coughing, lethargy, loss of appetite"

### 4. `heart_rate` (Nhịp tim)
- **Type**: `Integer`
- **Database**: `INTEGER`
- **Range**: >= 40 bpm
- **Validation**: @Min(40)
- **Required**: Optional

### 5. `symptom_duration` (Thời gian triệu chứng)
- **Type**: `Integer`
- **Database**: `INTEGER`
- **Range**: >= 0 days
- **Validation**: @Min(0)
- **Required**: Optional

### 6. `target_diagnosis` (Chẩn đoán)
- **Type**: `String`
- **Database**: `VARCHAR(100)`
- **Max Length**: 100 characters
- **Validation**: @Size(max = 100)
- **Required**: Optional
- **Example**: "Parvo Virus", "Respiratory Infection", "Heatstroke"

## Database Schema

### PostgreSQL

Schema file: `src/main/resources/db/postgresql/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS visits (
  id SERIAL PRIMARY KEY,
  pet_id INTEGER NOT NULL,
  visit_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  description VARCHAR(8192),
  -- Medical data fields
  temperature NUMERIC(4,1) CHECK (temperature >= 35.0 AND temperature <= 43.0),
  weight_kg NUMERIC(5,2) CHECK (weight_kg >= 0.1 AND weight_kg <= 100.0),
  symptoms_list VARCHAR(5000),
  heart_rate INTEGER CHECK (heart_rate >= 40),
  symptom_duration INTEGER CHECK (symptom_duration >= 0),
  target_diagnosis VARCHAR(100),
  CONSTRAINT fk_visits_pet FOREIGN KEY (pet_id) REFERENCES pets(id)
);
```

### Migration Script

Nếu bảng `visits` đã tồn tại, sử dụng migration script:
`src/main/resources/db/postgresql/migration_add_medical_fields.sql`

```bash
psql -U petclinic -d petclinic -f src/main/resources/db/postgresql/migration_add_medical_fields.sql
```

## Cấu hình

### PostgreSQL Profile

Sử dụng profile `postgresql` để kích hoạt PostgreSQL:

```bash
java -jar visits-service.jar --spring.profiles.active=postgresql
```

Hoặc trong `application.yml`:

```yaml
spring:
  profiles:
    active: postgresql
```

### Environment Variables

```bash
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=petclinic
export POSTGRES_USER=petclinic
export POSTGRES_PASSWORD=petclinic
```

## API Usage

### Tạo Visit với dữ liệu y tế

```bash
POST /owners/{ownerId}/pets/{petId}/visits
Content-Type: application/json

{
  "date": "2024-01-15",
  "description": "Regular checkup",
  "temperature": 39.5,
  "weightKg": 12.5,
  "symptomsList": "fever, coughing, lethargy",
  "heartRate": 120,
  "symptomDuration": 3,
  "targetDiagnosis": "Respiratory Infection"
}
```

### Response

```json
{
  "id": 1,
  "date": "2024-01-15",
  "description": "Regular checkup",
  "petId": 7,
  "temperature": 39.5,
  "weightKg": 12.5,
  "symptomsList": "fever, coughing, lethargy",
  "heartRate": 120,
  "symptomDuration": 3,
  "targetDiagnosis": "Respiratory Infection"
}
```

## Validation

Tất cả các trường đều có validation:
- **Temperature**: Phải trong khoảng 35.0 - 43.0°C
- **Weight**: Phải trong khoảng 0.1 - 100.0 kg
- **Heart Rate**: Phải >= 40 bpm
- **Symptom Duration**: Phải >= 0 days
- **Symptoms List**: Tối đa 5000 characters
- **Target Diagnosis**: Tối đa 100 characters

Nếu validation fail, API sẽ trả về HTTP 400 Bad Request với error message.

## Dependencies

PostgreSQL driver đã được thêm vào `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Testing

### Test với PostgreSQL

1. Start PostgreSQL:
```bash
docker run -d --name postgres-petclinic \
  -e POSTGRES_DB=petclinic \
  -e POSTGRES_USER=petclinic \
  -e POSTGRES_PASSWORD=petclinic \
  -p 5432:5432 \
  postgres:15
```

2. Run migration:
```bash
psql -U petclinic -d petclinic -f src/main/resources/db/postgresql/schema.sql
```

3. Start service với PostgreSQL profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgresql
```

## Notes

- Tất cả các trường y tế đều là **optional** để đảm bảo backward compatibility
- Validation được thực hiện ở cả entity level (JPA) và database level (CHECK constraints)
- Indexes đã được tạo cho `pet_id`, `visit_date`, và `target_diagnosis` để tối ưu query performance

---

**Ngày tạo**: 2024  
**Phiên bản**: 1.0

