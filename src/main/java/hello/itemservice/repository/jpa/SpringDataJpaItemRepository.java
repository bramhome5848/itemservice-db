package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 메서드 이름으로 쿼리 실행
 - 조건이 많으면 메서드 이름이 너무 길어짐
 - 조인 같은 복잡한 조건을 사용할 수 없음
 -> 간단한 경우에는 메서드 이름으로 쿼리 실행할 수 있지만 복잡할 경우 JPQL 쿼리를 작성하는 것이 좋음
 -> 메서드 이름으로 쿼리 실행시는 파라미터를 순서대로 입력하면 되지만, 쿼리 직접 실행시는 파라미터를 명시적으로 바인딩 해야함(@Param)
 */
public interface SpringDataJpaItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByItemNameLike(String itemName);

    List<Item> findByPriceLessThanEqual(Integer price);

    //쿼리 메서드(아래 메서드와 같은 기능 수행)
    List<Item> findByItemNameLikeAndPriceLessThanEqual(String itemName, Integer price);

    //쿼리 직접 실행
    @Query("select i from Item i where i.itemName like :itemName and i.price <= :price")
    List<Item> findItems(@Param("itemName") String itemName, @Param("price") Integer price);
}
