package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * @Repository 기능
 1. 컴포넌트 스캔의 대상이 됨
 2. 에외 변환 AOP 의 적용 대상이 됨
 - 스프링과 JPA 를 함께 사용하는 경우 스프링은 JPA 예외 변환기(PersistenceExceptionTranslator) 를 등록
 - 예외 변환 AOP 프록시는 JPA 관련 예외가 발생하면 JPA 예외 변환기를 통해 발생한 예외를 스프링 데이터 접근 예외로 변환(강의자료 그림 참고)
 - 서비스 계층 --- 예외 변환 AOP Proxy --- JpaItemRepository --- EntityManager
 */
@Slf4j
@Repository
@Transactional  //JPA 의 모든 데이터 변경은 트랜잭션 안에서 이루어져야 함(조회는 트랜잭션 없이 가능)
public class JpaItemRepositoryV1 implements ItemRepository {

    private final EntityManager em;

    public JpaItemRepositoryV1(EntityManager em) {
        this.em = em;
    }

    public Item save(Item item) {
        em.persist(item);
        return item;
    }

    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = em.find(Item.class, itemId);
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    public Optional<Item> findById(Long id) {
        Item item = em.find(Item.class, id);
        return Optional.ofNullable(item);
    }

    public List<Item> findAll(ItemSearchCond cond) {
        String jpql = "select i from Item i";

        Integer maxPrice = cond.getMaxPrice();
        String itemName = cond.getItemName();

        if(StringUtils.hasText(itemName) || maxPrice != null) {
            jpql += " where";
        }

        boolean andFlag = false;
        if(StringUtils.hasText(itemName)) {
            jpql += " i.itemName like concat('%', :itemName, '%')";
            andFlag = true;
        }

        if(maxPrice != null) {
            if(andFlag) {
                jpql += " and";
            }
            jpql += " i.price <= :maxPrice";
        }

        log.info("jpql = {}", jpql);

        TypedQuery<Item> query = em.createQuery(jpql, Item.class);
        if(StringUtils.hasText(itemName)) {
            query.setParameter("itemName", itemName);
        }
        if(maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }
}
