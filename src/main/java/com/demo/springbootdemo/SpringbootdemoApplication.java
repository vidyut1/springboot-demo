package com.demo.springbootdemo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@SpringBootApplication
@EnableJpaRepositories
@EnableAutoConfiguration
public class SpringbootdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootdemoApplication.class, args);
	}
}

class User {
	private Integer id;
	private String name;

	public User() {
	}

	public User(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

class HotelBooking {
	private Integer id;
	@JsonProperty("user_id")
	private Integer userId;
	@JsonProperty("booking_date")
	private String date;

	public HotelBooking() {
	}

	public HotelBooking(Integer id, Integer userId, String date) {
		this.id = id;
		this.userId = userId;
		this.date = date;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}

class SyncRequest {
	@JsonProperty("users")
	private List<User> userList;
	@JsonProperty("hotel_bookings")
	private List<HotelBooking> hotelBookingList;

	public SyncRequest(List<User> userList, List<HotelBooking> hotelBookingList) {
		this.userList = userList;
		this.hotelBookingList = hotelBookingList;
	}

	public List<User> getUserList() {
		return userList;
	}

	public void setUserList(List<User> userList) {
		this.userList = userList;
	}

	public List<HotelBooking> getHotelBookingList() {
		return hotelBookingList;
	}

	public void setHotelBookingList(List<HotelBooking> hotelBookingList) {
		this.hotelBookingList = hotelBookingList;
	}

	public SyncRequest() {
	}
}
@Entity
@Table(name = "users")
class UserEntity{
	@Id
    @Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name")
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

@Entity
@Table(name = "hotel_bookings")
class HotelBookingEntity{
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "booking_date")
	private String bookingDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getBookingDate() {
		return bookingDate;
	}

	public void setBookingDate(String bookingDate) {
		this.bookingDate = bookingDate;
	}
}
class EntityMapper {
	public static UserEntity createUserEntityFromUser(User user) {
		UserEntity userEntity = new UserEntity();
		userEntity.setName(user.getName());
		return userEntity;
	}
	public static HotelBookingEntity createHotelBookingEntityFromHotelBooking(HotelBooking hotelBooking) {
		HotelBookingEntity hotelBookingEntity = new HotelBookingEntity();
		hotelBookingEntity.setBookingDate(hotelBooking.getDate());
		hotelBookingEntity.setUserId(hotelBooking.getUserId());
		return hotelBookingEntity;
	}
}

@Repository
interface UserRepository extends JpaRepository<UserEntity, Long> { }

@Repository
interface HotelBookingRepository extends JpaRepository<HotelBookingEntity, Long> { }

@RestController
class SyncController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
    private HotelBookingRepository hotelBookingRepository;

	@PostMapping(path = "/sync")
	public String processSync(@RequestBody SyncRequest request) {
		long start = System.nanoTime();
		ExecutorService executorServiceUser = Executors.newCachedThreadPool();
		ExecutorService executorServiceHotel = Executors.newCachedThreadPool();
		for (User user: request.getUserList()) {
			executorServiceUser.execute(() -> {
				UserEntity entity = EntityMapper.createUserEntityFromUser(user);
				userRepository.save(entity);
			});
		}
		for (HotelBooking hotelBooking: request.getHotelBookingList()) {
			executorServiceHotel.execute(() -> {
				HotelBookingEntity entity = EntityMapper.createHotelBookingEntityFromHotelBooking(hotelBooking);
				hotelBookingRepository.save(entity);
			});
		}
		executorServiceHotel.shutdown();
		executorServiceUser.shutdown();
		try {
			executorServiceHotel.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			executorServiceUser.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {

		}
		long end = System.nanoTime();
		return "processed "+(end - start);
	}
}
