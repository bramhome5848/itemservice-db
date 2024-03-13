package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 이름 지정 바인딩
 - jdbcTemplate 를 기본으로 사용하면 파라미터를 순서대로 바인딩
 - 필드 추가, 수정과정에서 파라미터의 순서가 변경되어 문제가 발생하기 쉬움 -> 데이터 문제!!
 - 개발시 코드를 줄이는 편리함도 중요하지만, 모호함을 제거해서 코드를 명확하게 만드는 것이 유지보수 관점에서 중요!!
 - jdbcTemplate 은 NamedParameterJdbcTemplate 라는 이름을 지정해 파라미터를 바인딩 하는 기능을 제공

 * 이름 지정 바인딩에서 자주 사용하는 파라미터의 종류 3가지
 1. Map
 - findById() 코드에서 확인가능
 2. MapSqlParameterSource
 - Map 과 유사하고 SQL 타입을 지정할 수 있으며 SQL 에 좀 더 특화된 기능을 제공
 - SqlParameterSource 인터페이스 구현체
 - 메서드 체인을 통해 편리한 사용법 제공
 - update() 코드에서 확인가능
 3. BeanPropertySqlParameterSource
 - 자바빈 프로퍼티 규약을 통해 자동으로 파라미터 객체 생성
 ex) getXxx() -> xxx, getItemName() -> itemName
 - SqlParameterSource 인터페이스 구현체
 - save(), findAll() 에서 확인 가능

 * BeanPropertyRowMapper
 - ResultSet 의 결과를 받아서 자바빈 규약에 맞추어 데이터를 변환
 -> 객체 생성 후 데이터베이스에서 조회한 결과 이름을 기반으로 자바빈 프로퍼티 규약에 맞춘 메서드 호출
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;

    /**
     * dataSource 를 의존 관계 주입 받고 생성자 내부에서 NamedParameterJdbcTemplate 생성
     -> 스프링에서 jdbcTemplate 사용시 관례상 해당 방법을 많이 사용
     -> 물론 NamedParameterJdbcTemplate 을 스프링 빈으로 직접 등록하고 주입받아도 됨
     */
    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * NamedParameterJdbcTemplate -> 데이터베이스가 생성해주는 키를 매우 쉽게 조회하는 기능 제공
     */
    public Item save(Item item) {
        String sql = "insert into item (item_name, price, quantity) " +
                     "values (:itemName, :price, :quantity)";

        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(sql, param, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }

    /**
     * BeanPropertySqlParameterSource 를 사용하면 편리하지만 param Object 와 다른 파라미터를 함께 이용할 경우 사용할 수 없음
     -> Map 또는 MapSqlParameterSource 사용
     */
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item " +
                     "set item_name = :itemName, price  = :price, quantity = :quantity " +
                     "where id = :id";

        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);

        template.update(sql, param);
    }

    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";

        try {
            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        } catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";
        //동적쿼리
        if(StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;
        if(StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%', :itemName, '%')";
            andFlag = true;
        }

        if(maxPrice != null) {
            if(andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }

        log.info("sql = {}", sql);
        return template.query(sql, param, itemRowMapper());
    }

    private RowMapper<Item> itemRowMapper() {
        return BeanPropertyRowMapper.newInstance(Item.class);   //camel 변환 진원
    }
}
