package hello.itemservice.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity     //JPA 가 사용하는 객체 -> 엔티티
public class Item {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) //PK 생성 값을 데이터베이스에서 생성하는 방식
    private Long id;

    //@Column -> 객체의 필드를 테이블의 컬럼과 매핑
    //length = 10 -> JPA 매핑정보로 DDL 생성시 컬럼 길이 값으로 활용
    //생략시 필드명을 테이블 컬럼명으로 사용, 스프링 부트와 함께 사용시 필드명을 테이블 컬럼명으로 변경할 때 객체 필드의 카멜 케이스를 언더스코어로 자동 변경
    @Column(name = "item_name", length = 10)
    private String itemName;
    private Integer price;
    private Integer quantity;

    public Item() { //JPA 는 public 또는 protected 기본 생성자가 필수
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
