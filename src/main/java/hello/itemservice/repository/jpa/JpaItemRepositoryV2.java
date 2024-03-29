package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 의존 관계로 인한 문제
 - ItemService 는 ItemRepository 에 의존, ItemService 에서 SpringDataJpaItemRepository 를 그대로 사용할 수 없음
 - ItemService 가 SpringDataJpaItemRepository 를 직접 사용하도록 코드 수정하면 문제는 해결할 수 있음
 - 하지만 ItemService 의 코드 변경 없이 ItemService 가 ItemRepository 에 대한 의존을 유지하면서 DI 를 통해 구현기술을 변경하고 싶음
 -> JpaItemRepositoryV2 는 ItemRepository 와 SpringDataJpaItemRepository 사이를 맞추기 위한 어댑터처럼 사용

 * 예외 변환
 - 스프링 데이터 JPA 도 스프링 예외 추상화를 지원, 스프링 데이터 JPA 가 만들어주는 프록시에서 예외 변환을 처리하기 때문에 @Repository 가 없어도 예외 변환됨
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class JpaItemRepositoryV2 implements ItemRepository {

    private final SpringDataJpaItemRepository repository;

    public Item save(Item item) {
        return repository.save(item);
    }

    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = repository.findById(itemId).orElseThrow();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    public Optional<Item> findById(Long id) {
        return repository.findById(id);
    }

    public List<Item> findAll(ItemSearchCond cond) {
        Integer maxPrice = cond.getMaxPrice();
        String itemName = cond.getItemName();

        if(StringUtils.hasText(itemName) && maxPrice != null) {
            //return repository.findByItemNameLikeAndPriceLessThanEqual(itemName, maxPrice);
            return repository.findItems("%" + itemName + "%", maxPrice);
        } else if(StringUtils.hasText(itemName)) {
            return repository.findByItemNameLike("%" + itemName + "%");
        } else if(maxPrice != null) {
            return repository.findByPriceLessThanEqual(maxPrice);
        } else {
            return repository.findAll();
        }
    }
}
