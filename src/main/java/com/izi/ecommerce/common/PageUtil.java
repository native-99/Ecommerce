package com.izi.ecommerce.common;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PageUtil {

    public static List<Sort.Order> parseSortOrderRequest(String[] sort) {
        return parseSortOrderRequest(sort, PageUtil::getOrderSortProperty);
    }

    public static List<Sort.Order> parseProductSortOrderRequest(String[] sort) {
        return parseSortOrderRequest(sort, PageUtil::getProductSortProperty);
    }

    public static List<Sort.Order> parseSortOrderRequest(
            String[] sort,
            Function<String, String> sortPropertyMapper
    ) {
        List<Sort.Order> orders = new ArrayList<>();

        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] sortParts = sortOrder.split(",");
                orders.add(new Sort.Order(
                        getSortDirection(sortParts.length > 1 ? sortParts[1] : "asc"),
                        sortPropertyMapper.apply(sortParts[0])
                ));
            }
        } else {
            orders.add(new Sort.Order(
                    getSortDirection(sort.length > 1 ? sort[1] : "asc"),
                    sortPropertyMapper.apply(sort[0])
            ));
        }

        return orders;
    }

    private static Sort.Direction getSortDirection(String direction) {
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }

        return Sort.Direction.ASC;
    }

    private static String getOrderSortProperty(String property) {
        return switch (property) {
            case "order_id" -> "orderId";
            case "user_id" -> "userId";
            case "order_date" -> "orderDate";
            case "total_amount" -> "totalAmount";
            default -> property;
        };
    }

    private static String getProductSortProperty(String property) {
        return switch (property) {
            case "product_id" -> "productId";
            case "stock_quantity" -> "stockQuantity";
            case "created_at" -> "createdAt";
            case "updated_at" -> "updatedAt";
            default -> property;
        };
    }
}
