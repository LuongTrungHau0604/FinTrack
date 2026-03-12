package com.example.myapplication.data.datasource; // Make sure package is correct

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.data.model.Budget;
import com.example.myapplication.data.model.UserProfile;
import com.example.myapplication.data.model.Loan;
import com.example.myapplication.data.model.PaymentReminder;
import com.example.myapplication.data.model.Repayment;
import com.example.myapplication.data.model.SharedAccountRef;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;

import com.example.myapplication.data.model.Account; // Adjust model import path
import com.example.myapplication.data.model.Category;
import com.example.myapplication.data.model.Transaction;
import com.example.myapplication.data.model.FirebaseModelBase;

import com.example.myapplication.data.model.Profile;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDAO {

    private static final String TAG = "FirebaseDAO_Firestore";
    public static final String EMAIL_TO_UID_COLLECTION = "email_to_uid";
    private static FirebaseDAO instance;

    private final FirebaseFirestore db;
    public static final String SHARED_ACCOUNT_REFS_COLLECTION = "sharedAccountRefs"; // <<< Tên collection tham chiếu


    public static final String USERS_COLLECTION = "users";
    public static final String TRANSACTIONS_COLLECTION = "transactions";
    public static final String CATEGORIES_COLLECTION = "categories";
    public static final String ACCOUNTS_COLLECTION = "accounts";
    public static final String BUDGETS_COLLECTION = "budgets";         // Added
    public static final String LOANS_COLLECTION = "loans";             // Added
    public static final String REMINDERS_COLLECTION = "payment_reminders"; // Added
    public static final String REPAYMENTS_SUBCOLLECTION = "repayments"; // Added as subcollection of loans
    // public static final String BUDGETS_COLLECTION = "budgets";

    public FirebaseDAO() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "FirebaseDAO (Firestore) initialized.");
    }

    /**
     * Gets the singleton instance of FirebaseDAO
     * @return The FirebaseDAO instance
     */
    public static FirebaseDAO getInstance() {
        if (instance == null) {
            synchronized (FirebaseDAO.class) {
                if (instance == null) {
                    instance = new FirebaseDAO();
                }
            }
        }
        return instance;
    }
    public DocumentReference getUserDocumentRef(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Invalid User ID provided to getUserDocumentRef.");
            return null;
        }
        return db.collection(USERS_COLLECTION).document(userId);
    }

    public CollectionReference getUserSubCollectionRef(String userId, String subCollection) {
        DocumentReference userDocRef = getUserDocumentRef(userId);
        if (userDocRef == null || subCollection == null || subCollection.isEmpty()) {
            Log.e(TAG, "Invalid User ID or SubCollection name provided to getUserSubCollectionRef.");
            return null;
        }
        return userDocRef.collection(subCollection);
    }

    public void createInitialUserData(String userId, String email, String displayName, OnDataInitializedListener listener) {
        DocumentReference userDocRef = getUserDocumentRef(userId);
        if (userDocRef == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid User ID for initializing data."));
            return;
        }

        // Tạo dữ liệu profile
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("displayName", displayName != null ? displayName : "New User");
        profileData.put("createdAt", FieldValue.serverTimestamp());
        // *** THÊM DÒNG NÀY ĐỂ LƯU EMAIL ***
        if (email != null && !email.isEmpty()) { // Kiểm tra email hợp lệ trước khi lưu
            profileData.put("email", email);
        }
        // *** KẾT THÚC THÊM DÒNG ***

        // Tạo dữ liệu settings
        Map<String, Object> settingsData = new HashMap<>();
        settingsData.put("defaultCurrency", "VND");
        settingsData.put("language", "vi");
        settingsData.put("darkModeEnabled", false);

        // Tạo dữ liệu gốc cho document user
        Map<String, Object> initialUserData = new HashMap<>();
        initialUserData.put("profile", profileData); // Đã bao gồm email (nếu có)
        initialUserData.put("settings", settingsData);
        // Tạo sẵn các map rỗng cho subcollections (nếu bạn muốn chúng xuất hiện ngay)
        initialUserData.put(ACCOUNTS_COLLECTION, new HashMap<>());
        initialUserData.put(CATEGORIES_COLLECTION, new HashMap<>()); // Sẽ được ghi đè nếu createDefaultCategories được gọi
        initialUserData.put(TRANSACTIONS_COLLECTION, new HashMap<>());
        initialUserData.put(SHARED_ACCOUNT_REFS_COLLECTION, new HashMap<>());
        initialUserData.put(LOANS_COLLECTION, new HashMap<>()); // Thêm node rỗng
        initialUserData.put(BUDGETS_COLLECTION, new HashMap<>()); // Thêm node rỗng
        initialUserData.put(REMINDERS_COLLECTION, new HashMap<>());// Thêm node rỗng


        // Ghi dữ liệu ban đầu vào Firestore
        userDocRef.set(initialUserData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Initial user document created successfully for " + userId);
                    // Quyết định xem có cần tạo category mặc định không
                    // Nếu bạn muốn tạo sẵn categories, hãy bỏ comment dòng dưới
                    createDefaultCategories(userId, listener); // Gọi hàm này nếu muốn tạo sẵn categories
                    // Nếu không tạo default categories, báo thành công ngay:
                    // if(listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create initial user document for " + userId, e);
                    if(listener != null) listener.onFailure(e);
                });
    }

    private void createDefaultCategories(String userId, OnDataInitializedListener mainListener) {
        CollectionReference categoriesRef = getUserSubCollectionRef(userId, CATEGORIES_COLLECTION);
        if(categoriesRef == null) {
            if(mainListener != null) mainListener.onSuccess();
            return;
        }

        WriteBatch batch = db.batch();

        Category foodCat = new Category();
        foodCat.setName("Ăn uống"); foodCat.setType("expense"); foodCat.setIcon("ic_food_drink"); foodCat.setColor("#FF9800"); foodCat.setCustom(false); foodCat.setParentCategoryId(null);
        DocumentReference foodRef = categoriesRef.document("default_cat_food");
        batch.set(foodRef, foodCat);

        Category salaryCat = new Category();
        salaryCat.setName("Lương"); salaryCat.setType("income"); salaryCat.setIcon("ic_salary"); salaryCat.setColor("#4CAF50"); salaryCat.setCustom(false); salaryCat.setParentCategoryId(null);
        DocumentReference salaryRef = categoriesRef.document("default_cat_salary");
        batch.set(salaryRef, salaryCat);


        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Default categories created for user " + userId);
                    if(mainListener != null) mainListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create default categories for user " + userId, e);
                    if(mainListener != null) mainListener.onSuccess();
                });
    }

    public void addTransaction(String userId, Transaction transaction, OnTransactionAddedListener listener) {
        // Lưu thông tin cần thiết trước khi bắt đầu transaction
        final String accountId = transaction.getAccountId();
        final double amount = transaction.getAmount();
        final boolean isIncome = "income".equalsIgnoreCase(transaction.getType());

        // Kiểm tra tham chiếu transaction
        CollectionReference transactionsRef = getUserSubCollectionRef(userId, TRANSACTIONS_COLLECTION);
        if (transactionsRef == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference for adding transaction."));
            return;
        }

        // Lấy tham chiếu tới tài khoản
        DocumentReference accountRef = getUserSubCollectionRef(userId, ACCOUNTS_COLLECTION).document(accountId);

        // Sử dụng transaction của Firestore để đảm bảo tính nhất quán
        FirebaseFirestore.getInstance().runTransaction(transaction1 -> {
            // Đọc thông tin tài khoản hiện tại
            DocumentSnapshot accountSnapshot = transaction1.get(accountRef);
            if (!accountSnapshot.exists()) {
                throw new FirebaseFirestoreException("Account not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Account account = accountSnapshot.toObject(Account.class);
            if (account == null) {
                throw new FirebaseFirestoreException("Failed to parse account data", FirebaseFirestoreException.Code.INTERNAL);
            }

            // Tính toán số dư mới
            double currentBalance = account.getCurrentBalance();
            double amountChange = isIncome ? amount : -amount;  // Income thì cộng, Expense thì trừ
            double newBalance = currentBalance + amountChange;

            // Thêm transaction mới
            DocumentReference newTransactionRef = transactionsRef.document();
            transaction1.set(newTransactionRef, transaction);

            // Cập nhật số dư tài khoản
            transaction1.update(accountRef, "currentBalance", newBalance);

            // Trả về ID của transaction mới
            return newTransactionRef.getId();
        }).addOnSuccessListener(transactionId -> {
            if(listener != null) listener.onSuccess(transactionId);
        }).addOnFailureListener(e -> {
            if(listener != null) listener.onFailure(e);
        });
    }

    // *** SỬA LẠI LISTENER Ở ĐÂY ***
    public void getAllTransactions(String userId, OnTransactionsRetrievedListener listener) { // Dùng OnTransactionsRetrievedListener (có s)
        CollectionReference transactionsRef = getUserSubCollectionRef(userId, TRANSACTIONS_COLLECTION);
        if (transactionsRef == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference for getting transactions."));
            return;
        }
        transactionsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                try {
                                    Transaction transaction = document.toObject(Transaction.class);
                                    if (transaction != null) {
                                        transaction.setFirebaseId(document.getId());
                                        transactions.add(transaction);
                                    }
                                } catch (Exception e) { Log.e(TAG, "Error converting transaction doc: " + document.getId(), e); }
                            }
                        }
                        if(listener != null) listener.onSuccess(transactions); // Truyền List<Transaction>
                    } else {
                        if(listener != null) listener.onFailure(task.getException());
                    }
                });
    }

    // *** SỬA LẠI LISTENER Ở ĐÂY ***
    public void getTransactionById(String userId, String transactionId, OnTransactionRetrievedListener listener) { // Dùng OnTransactionRetrievedListener (không có s)
        CollectionReference transactionsRef = getUserSubCollectionRef(userId, TRANSACTIONS_COLLECTION);
        if (transactionsRef == null || transactionId == null || transactionId.isEmpty()) {
            if (listener != null) listener.onFailure(new Exception("Invalid reference or ID for getting transaction."));
            return;
        }
        DocumentReference docRef = transactionsRef.document(transactionId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                Transaction transaction = null;
                if (document != null && document.exists()) {
                    try {
                        transaction = document.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setFirebaseId(document.getId());
                        } else {
                            Log.w(TAG, "Transaction data exists but could not be parsed for ID: " + transactionId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting transaction document: " + transactionId, e);
                        if (listener != null) listener.onFailure(e);
                        return;
                    }
                } else {
                    Log.w(TAG, "Transaction not found for ID: " + transactionId);
                }
                if (listener != null) listener.onSuccess(transaction); // Truyền Transaction (có thể là null)
            } else {
                Log.e(TAG, "Failed to retrieve transaction: " + transactionId, task.getException());
                if (listener != null) listener.onFailure(task.getException());
            }
        });
    }


    public void updateTransaction(String userId, String transactionId, Transaction updatedTransactionData, OnTransactionUpdatedListener listener) {
        CollectionReference transactionsRef = getUserSubCollectionRef(userId, TRANSACTIONS_COLLECTION);
        if (transactionsRef == null || transactionId == null || transactionId.isEmpty() || updatedTransactionData == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference, ID, or data for updating transaction."));
            return;
        }
        DocumentReference docRef = transactionsRef.document(transactionId);
        docRef.set(updatedTransactionData)
                .addOnSuccessListener(aVoid -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }

    public void deleteTransaction(String userId, String transactionId, OnTransactionDeletedListener listener) {
        CollectionReference transactionsRef = getUserSubCollectionRef(userId, TRANSACTIONS_COLLECTION);
        if (transactionsRef == null || transactionId == null || transactionId.isEmpty()) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference or ID for deleting transaction."));
            return;
        }
        DocumentReference docRef = transactionsRef.document(transactionId);
        docRef.delete()
                .addOnSuccessListener(aVoid -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }

    public void addCategory(String userId, Category category, OnCategoryAddedListener listener) {
        CollectionReference categoriesRef = getUserSubCollectionRef(userId, CATEGORIES_COLLECTION);
        if (categoriesRef == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference for adding category."));
            return;
        }
        categoriesRef.add(category)
                .addOnSuccessListener(docRef -> { if(listener != null) listener.onSuccess(docRef.getId()); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }

    public void getAllCategories(String userId, OnCategoriesRetrievedListener listener) {
        CollectionReference categoriesRef = getUserSubCollectionRef(userId, CATEGORIES_COLLECTION);
        if (categoriesRef == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference for getting categories."));
            return;
        }
        categoriesRef.orderBy("name")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Category> categories = new ArrayList<>();
                        QuerySnapshot result = task.getResult();
                        if (result != null) {
                            for(DocumentSnapshot doc : result.getDocuments()){
                                try {
                                    Category cat = doc.toObject(Category.class);
                                    if(cat != null) {
                                        cat.setFirebaseId(doc.getId());
                                        categories.add(cat);
                                    }
                                } catch(Exception e){ Log.e(TAG, "Error converting category doc: " + doc.getId(), e); }
                            }
                        }
                        if(listener != null) listener.onSuccess(categories);
                    } else {
                        if(listener != null) listener.onFailure(task.getException());
                    }
                });
    }

    public void getCategoryById(String userId, String categoryId, OnCategoryRetrievedListener listener) {
        CollectionReference categoriesRef = getUserSubCollectionRef(userId, CATEGORIES_COLLECTION);
        if (categoriesRef == null || categoryId == null || categoryId.isEmpty()) {
            if (listener != null) listener.onFailure(new Exception("Invalid reference or ID for getting category."));
            return;
        }
        DocumentReference docRef = categoriesRef.document(categoryId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                Category category = null;
                if (document != null && document.exists()) {
                    try {
                        category = document.toObject(Category.class);
                        if (category != null) {
                            category.setFirebaseId(document.getId());
                        } else { Log.w(TAG, "Category data null after parsing: " + categoryId); }
                    } catch (Exception e) { Log.e(TAG, "Error converting category doc: " + categoryId, e); if(listener!=null) listener.onFailure(e); return;}
                } else { Log.w(TAG, "Category not found: " + categoryId); }
                if(listener!=null) listener.onSuccess(category);
            } else {
                Log.e(TAG, "Failed to retrieve category: " + categoryId, task.getException());
                if(listener!=null) listener.onFailure(task.getException());
            }
        });
    }


    public void updateCategory(String userId, String categoryId, Category updatedCategoryData, OnCategoryUpdatedListener listener) {
        CollectionReference categoriesRef = getUserSubCollectionRef(userId, CATEGORIES_COLLECTION);
        if (categoriesRef == null || categoryId == null || categoryId.isEmpty() || updatedCategoryData == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference, ID, or data for updating category."));
            return;
        }
        DocumentReference docRef = categoriesRef.document(categoryId);
        docRef.set(updatedCategoryData)
                .addOnSuccessListener(aVoid -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }


    public void deleteCategory(String userId, String categoryId, OnCategoryDeletedListener listener) {
        CollectionReference categoriesRef = getUserSubCollectionRef(userId, CATEGORIES_COLLECTION);
        if (categoriesRef == null || categoryId == null || categoryId.isEmpty()) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference or ID for deleting category."));
            return;
        }
        DocumentReference docRef = categoriesRef.document(categoryId);
        docRef.delete()
                .addOnSuccessListener(aVoid -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }

    public void addAccount(String userId, Account account, OnAccountAddedListener listener) {
        CollectionReference accountsRef = getUserSubCollectionRef(userId, ACCOUNTS_COLLECTION);
        if (accountsRef == null) { if(listener != null) listener.onFailure(new Exception("Invalid ref")); return; }

        // --- GÁN OWNER ID KHI TẠO MỚI ---
        account.setOwnerId(userId);
        // Khởi tạo sharedWithUids nếu là null (an toàn)
        if (account.getSharedWithUids() == null) {
            account.setSharedWithUids(new ArrayList<>());
        }
        // --- KẾT THÚC GÁN OWNER ID ---

        accountsRef.add(account) // Lưu đối tượng đã có ownerId
                .addOnSuccessListener(docRef -> { if(listener != null) listener.onSuccess(docRef.getId()); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }

    public void getAllAccounts(String userId, OnAccountsRetrievedListener listener) {
        CollectionReference accountsRef = getUserSubCollectionRef(userId, ACCOUNTS_COLLECTION);
        if (accountsRef == null) {
            if(listener != null) listener.onFailure(new Exception("Invalid reference for getting accounts."));
            return;
        }
        accountsRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Account> accounts = new ArrayList<>();
                        QuerySnapshot result = task.getResult();
                        if(result != null){
                            for(DocumentSnapshot doc : result.getDocuments()){
                                try {
                                    Account acc = doc.toObject(Account.class);
                                    if(acc != null){
                                        acc.setFirebaseId(doc.getId());
                                        accounts.add(acc);
                                    }
                                } catch(Exception e){ Log.e(TAG, "Error converting account doc: " + doc.getId(), e); }
                            }
                        }
                        if(listener != null) listener.onSuccess(accounts);
                    } else {
                        if(listener != null) listener.onFailure(task.getException());
                    }
                });
    }

    public void getAccountById(String userId, String accountId, OnAccountRetrievedListener listener) {
        CollectionReference accountsRef = getUserSubCollectionRef(userId, ACCOUNTS_COLLECTION);
        if (accountsRef == null || accountId == null || accountId.isEmpty()) {
            if (listener != null) listener.onFailure(new Exception("Invalid reference or ID for getting account."));
            return;
        }
        DocumentReference docRef = accountsRef.document(accountId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                Account account = null;
                if (document != null && document.exists()) {
                    try {
                        account = document.toObject(Account.class);
                        if (account != null) {
                            account.setFirebaseId(document.getId());
                        } else { Log.w(TAG, "Account data exists but could not be parsed for ID: " + accountId); }
                    } catch (Exception e) { Log.e(TAG, "Error converting account doc: " + accountId, e); if(listener!=null) listener.onFailure(e); return;}
                } else { Log.w(TAG, "Account not found: " + accountId); }
                if(listener!=null) listener.onSuccess(account);
            } else {
                Log.e(TAG, "Failed to retrieve account: " + accountId, task.getException());
                if(listener!=null) listener.onFailure(task.getException());
            }
        });
    }


    public void updateAccount(String userId, String accountId, Account updatedAccountData, OnAccountUpdatedListener listener) {
        CollectionReference accountsRef = getUserSubCollectionRef(userId, ACCOUNTS_COLLECTION);
        if (accountsRef == null || accountId == null || accountId.isEmpty() || updatedAccountData == null) {
            if (listener != null) listener.onFailure(new Exception("Invalid reference, ID, or data for updating account."));
            return;
        }
        DocumentReference docRef = accountsRef.document(accountId);
        docRef.set(updatedAccountData)
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }


    public void deleteAccount(String userId, String accountId, OnAccountDeletedListener listener) {
        CollectionReference accountsRef = getUserSubCollectionRef(userId, ACCOUNTS_COLLECTION);
        if (accountsRef == null || accountId == null || accountId.isEmpty()) {
            if (listener != null) listener.onFailure(new Exception("Invalid reference or ID for deleting account."));
            return;
        }
        DocumentReference docRef = accountsRef.document(accountId);
        docRef.delete()
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }




    // --- NEW: Loan Methods ---
    public void addLoan(String userId, Loan loan, OnLoanAddedListener listener) {
        Log.d(TAG, "Calling addLoan/updateLoan for user: " + userId);
        CollectionReference loansRef = getUserSubCollectionRef(userId, LOANS_COLLECTION);
        if (loansRef == null) { if(listener != null) listener.onFailure(new Exception("Invalid reference for adding loan.")); return; }
        // Initialize remaining balance if not set
        if (loan.getCurrentBalance() == 0 && loan.getInitialAmount() > 0) {
            loan.setCurrentBalance(loan.getInitialAmount());
        }
        loansRef.add(loan)
                .addOnSuccessListener(docRef -> { if(listener != null) listener.onSuccess(docRef.getId()); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }

    public void getAllLoans(String userId, OnLoansRetrievedListener listener) {
        CollectionReference loansRef = getUserSubCollectionRef(userId, LOANS_COLLECTION);
        if (loansRef == null) { if(listener != null) listener.onFailure(new Exception("Invalid reference for getting loans.")); return; }
        loansRef.orderBy("startDate", Query.Direction.DESCENDING) // Example order
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Loan> loans = new ArrayList<>();
                        QuerySnapshot result = task.getResult();
                        if (result != null) {
                            for (DocumentSnapshot doc : result.getDocuments()) {
                                try { Loan l = doc.toObject(Loan.class); if (l != null) { l.setFirebaseId(doc.getId()); loans.add(l); } }
                                catch (Exception e) { Log.e(TAG, "Error converting loan doc: " + doc.getId(), e); }
                            }
                        }
                        if(listener != null) listener.onSuccess(loans);
                    } else {
                        if(listener != null) listener.onFailure(task.getException());
                    }
                });
    }

    public void updateLoan(String userId, String loanId, Loan updatedLoanData, OnLoanUpdatedListener listener) {
        Log.d(TAG, "Calling addLoan/updateLoan for user: " + userId);
        CollectionReference loansRef = getUserSubCollectionRef(userId, LOANS_COLLECTION);
        if (loansRef == null || loanId == null || loanId.isEmpty() || updatedLoanData == null) { if(listener != null) listener.onFailure(new Exception("Invalid ref/ID/data for update.")); return; }
        loansRef.document(loanId).set(updatedLoanData)
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }

    public void deleteLoan(String userId, String loanId, OnLoanDeletedListener listener) {
        CollectionReference loansRef = getUserSubCollectionRef(userId, LOANS_COLLECTION);
        if (loansRef == null || loanId == null || loanId.isEmpty()) { if(listener != null) listener.onFailure(new Exception("Invalid ref/ID for delete.")); return; }

        // Consider deleting repayments first using a batch or Cloud Function
        // For simplicity here, just delete the loan doc
        loansRef.document(loanId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG,"Loan deleted: "+ loanId + ". Remember to handle/delete repayments if necessary.");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }

    public void getLoanById(String userId, String loanId, OnLoanRetrievedListener listener) {
        CollectionReference loansRef = getUserSubCollectionRef(userId, LOANS_COLLECTION);
        if (loansRef == null || loanId == null || loanId.isEmpty()) {
            if (listener != null) listener.onFailure(new Exception("Invalid reference or ID for getting loan."));
            return;
        }
        DocumentReference docRef = loansRef.document(loanId);
        Log.d(TAG, "Attempting to retrieve loan: " + loanId + " for user: " + userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                Loan loan = null;
                if (document != null && document.exists()) {
                    try {
                        loan = document.toObject(Loan.class);
                        if (loan != null) {
                            loan.setFirebaseId(document.getId());
                            Log.d(TAG, "Loan retrieved successfully: " + loanId);
                        } else { Log.w(TAG, "Loan data null after parsing: " + loanId); }
                    } catch (Exception e) { Log.e(TAG, "Error converting loan doc: " + loanId, e); if(listener!=null) listener.onFailure(e); return;}
                } else { Log.w(TAG, "Loan not found: " + loanId); }
                if(listener!=null) listener.onSuccess(loan); // Pass loan or null
            } else {
                Log.e(TAG, "Failed to retrieve loan: " + loanId, task.getException());
                if(listener!=null) listener.onFailure(task.getException());
            }
        });
    }


    // --- NEW: Shared Account Management Methods ---
    public void findUserByEmail(String email, OnUserFoundListener listener) {
        if (email == null || email.isEmpty()) {
            if(listener != null) listener.onFailure(new Exception("Email is empty"));
            return;
        }
        // Email trong Firestore thường được lưu trữ chính xác như người dùng nhập
        // hoặc đã được chuẩn hóa bởi Firebase Auth (thường là chữ thường).
        // Thực hiện query chữ thường để tăng khả năng khớp.
        String normalizedEmail = email.trim().toLowerCase();

        Log.d(TAG, "Querying for user with profile.email == " + normalizedEmail);

        db.collection(USERS_COLLECTION)
                .whereEqualTo("profile.email", normalizedEmail) // Query trường email bên trong map profile
                .limit(1) // Chỉ cần tìm 1 người dùng khớp
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Tìm thấy ít nhất một người dùng khớp
                            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0); // Lấy document đầu tiên
                            String foundUid = userDoc.getId();
                            Log.d(TAG, "Found user via email query. UID: " + foundUid);
                            if (listener != null) listener.onSuccess(foundUid);
                        } else {
                            // Không tìm thấy người dùng nào có email này
                            Log.w(TAG, "User not found for email via query: " + normalizedEmail);
                            if (listener != null) listener.onFailure(new Exception("User with that email not found."));
                        }
                    } else {
                        // Lỗi khi thực hiện query (có thể do PERMISSION_DENIED nếu rules sai)
                        Log.e(TAG, "Error finding user by email query: " + normalizedEmail, task.getException());
                        if (listener != null) listener.onFailure(task.getException());
                    }
                });
    }

    /**
     * Thêm quyền truy cập tài khoản cho người dùng khác.
     * Cập nhật list sharedWithUids của tài khoản gốc VÀ tạo tham chiếu trong sharedAccountRefs của người được mời.
     */
    public void shareAccountWithUser(String ownerId, String accountId, String sharedWithUid, String accountName, OnShareUpdatedListener listener) {
        if (ownerId == null || accountId == null || sharedWithUid == null || accountName == null) {
            if(listener != null) listener.onFailure(new Exception("Missing required IDs or name for sharing"));
            return;
        }

        DocumentReference accountDocRef = getUserSubCollectionRef(ownerId, ACCOUNTS_COLLECTION).document(accountId);

        // First update the account to add the shared user
        accountDocRef.update("sharedWithUids", FieldValue.arrayUnion(sharedWithUid))
                .addOnSuccessListener(aVoid -> {
                    // After successfully updating the account, create the reference for the shared user
                    CollectionReference sharedUserRefsCol = getUserSubCollectionRef(sharedWithUid, SHARED_ACCOUNT_REFS_COLLECTION);
                    if (sharedUserRefsCol == null) {
                        if(listener != null) listener.onFailure(new Exception("Invalid reference for shared user"));
                        return;
                    }

                    SharedAccountRef refData = new SharedAccountRef(ownerId, accountName);
                    DocumentReference newRefDoc = sharedUserRefsCol.document(accountId);

                    newRefDoc.set(refData)
                            .addOnSuccessListener(aVoid2 -> {
                                if(listener != null) listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                // Rollback the account update if reference creation fails
                                accountDocRef.update("sharedWithUids", FieldValue.arrayRemove(sharedWithUid));
                                if(listener != null) listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if(listener != null) listener.onFailure(e);
                });
    }





    public void toggleDarkMode(boolean enabled) {
        // Apply the theme change immediately
        int mode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);

        // Save preference to Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserProfile partialUpdate = new UserProfile();
            partialUpdate.setUid(currentUser.getUid());
            partialUpdate.setDarkModeEnabled(enabled);

            FirebaseDAO.getInstance().updateUserProfileField(partialUpdate, "darkModeEnabled", null);
        }
    }

    /**
     * Xóa quyền truy cập tài khoản của người dùng khác.
     * Xóa UID khỏi list sharedWithUids VÀ xóa tham chiếu trong sharedAccountRefs.
     */
    public void unshareAccountWithUser(String ownerId, String accountId, String sharedWithUid, OnShareUpdatedListener listener) {
        // ... (Kiểm tra null các tham số như cũ) ...
        DocumentReference accountDocRef = getUserSubCollectionRef(ownerId, ACCOUNTS_COLLECTION).document(accountId);
        DocumentReference sharedUserRefDoc = getUserSubCollectionRef(sharedWithUid, SHARED_ACCOUNT_REFS_COLLECTION).document(accountId);
        // ... (Kiểm tra null các ref như cũ) ...

        WriteBatch batch = db.batch();
        batch.update(accountDocRef, "sharedWithUids", FieldValue.arrayRemove(sharedWithUid));
        batch.delete(sharedUserRefDoc);
        batch.commit()
                .addOnSuccessListener(aVoid -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(e); });
    }


    /**
     * Lấy danh sách các tài khoản tham chiếu mà người dùng này được chia sẻ.
     */
    public void getSharedAccountReferences(String userId, OnSharedAccountRefsRetrievedListener listener) {
        CollectionReference refsRef = getUserSubCollectionRef(userId, SHARED_ACCOUNT_REFS_COLLECTION);
        if (refsRef == null) { if(listener != null) listener.onFailure(new Exception("Invalid ref")); return; }

        refsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<SharedAccountRef> refs = new ArrayList<>();
                QuerySnapshot result = task.getResult();
                if (result != null) {
                    for(DocumentSnapshot doc : result.getDocuments()){
                        try {
                            SharedAccountRef ref = doc.toObject(SharedAccountRef.class);
                            if(ref != null){
                                ref.setSharedAccountId(doc.getId()); // Gán ID tài khoản gốc vào tham chiếu
                                refs.add(ref);
                            }
                        } catch(Exception e){ Log.e(TAG,"Convert err shared ref",e);}
                    }
                }
                if(listener != null) listener.onSuccess(refs);
            } else { if(listener != null) listener.onFailure(task.getException()); }
        });
    }


    // --- Transaction Methods (Cập nhật để xử lý số dư tài khoản gốc) ---
    // Hàm addTransaction, updateTransaction, deleteTransaction cần được gọi với ownerId và accountId
    // của tài khoản gốc khi giao dịch liên quan đến tài khoản được chia sẻ.
    // Logic cập nhật số dư cũng phải cập nhật vào document tài khoản gốc.

    // Ví dụ: Cập nhật số dư (cần ownerId)
    public void updateAccountBalance(String ownerUserId, String accountId, double newBalance, OnAccountUpdatedListener listener) {
        // Sửa lại để dùng ownerId thay vì userId hiện tại nếu là tài khoản chia sẻ
        CollectionReference accountsRef = getUserSubCollectionRef(ownerUserId, ACCOUNTS_COLLECTION); // Dùng ownerId
        if (accountsRef == null || accountId == null || accountId.isEmpty()) { /* ... handle error ... */ return; }
        DocumentReference docRef = accountsRef.document(accountId);
        docRef.update("currentBalance", newBalance)
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }



    public void getUserProfile(String userId, OnProfileLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        db.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // Get the "profile" field which should contain the Profile object data
                            Object profileData = documentSnapshot.get("profile");

                            if (profileData instanceof Map) {
                                // Convert the map to a Profile object
                                Map<String, Object> profileMap = (Map<String, Object>) profileData;
                                Profile profile = new Profile();

                                // Extract display name if it exists
                                if (profileMap.containsKey("displayName")) {
                                    profile.setDisplayName((String) profileMap.get("displayName"));
                                }

                                // Extract created timestamp if it exists
                                if (profileMap.containsKey("createdAt")) {
                                    Object createdAtObj = profileMap.get("createdAt");
                                    if (createdAtObj instanceof Timestamp) {
                                        profile.setCreatedAt((Timestamp) createdAtObj);
                                    }
                                }

                                listener.onSuccess(profile);
                            } else {
                                // If no profile field exists or it's not a map, create an empty profile
                                listener.onSuccess(new Profile());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing profile data", e);
                            listener.onFailure(e);
                        }
                    } else {
                        // Document doesn't exist, return null profile
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile", e);
                    listener.onFailure(e);
                });
    }

    /**
     * Updates a user's display name in Firestore
     *
     * @param userId ID of the user to update
     * @param displayName New display name
     * @param listener Callback for operation result
     */
    public void updateUserDisplayName(String userId, String displayName, OnCompleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(userId);

        userDocRef.update("profile.displayName", displayName)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Display name updated successfully");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating display name", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    /**
     * Re-authenticates the current user with their email and password
     * Required before sensitive operations like changing password or deleting account
     *
     * @param email The user's email
     * @param password The user's current password
     * @param listener Callback to handle the result of the operation
     */
    public void reauthenticateUser(String email, String password, OnCompleteListener listener) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Email and password cannot be empty"));
            }
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (listener != null) {
                listener.onFailure(new IllegalStateException("No authenticated user found"));
            }
            return;
        }

        // Create credential with provided email and password
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        // Re-authenticate the user
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User re-authenticated successfully");
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Re-authentication failed", e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Deletes all user data from Firestore
     * This should be called before deleting the user's authentication account
     *
     * @param userId ID of the user whose data to delete
     * @param listener Callback for operation result
     */
    public void deleteUserData(String userId, OnCompleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        // Show that we're starting the deletion process
        Log.d(TAG, "Starting deletion of user data for user: " + userId);

        // Get a reference to the user document
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(userId);

        // Get all collections that may contain user data
        db.collection("accounts").whereEqualTo("ownerId", userId).get()
                .addOnSuccessListener(accountSnapshots -> {
                    db.collection("transactions").whereEqualTo("userId", userId).get()
                            .addOnSuccessListener(transactionSnapshots -> {
                                db.collection("categories").whereEqualTo("userId", userId).get()
                                        .addOnSuccessListener(categorySnapshots -> {
                                            db.collection("budgets").whereEqualTo("userId", userId).get()
                                                    .addOnSuccessListener(budgetSnapshots -> {
                                                        db.collection("loans").whereEqualTo("userId", userId).get()
                                                                .addOnSuccessListener(loanSnapshots -> {
                                                                    // Start a batch operation to delete all user data
                                                                    db.runBatch(batch -> {
                                                                        // Delete user document
                                                                        batch.delete(userDocRef);

                                                                        // Delete all accounts
                                                                        for (DocumentSnapshot doc : accountSnapshots) {
                                                                            batch.delete(doc.getReference());
                                                                        }

                                                                        // Delete all transactions
                                                                        for (DocumentSnapshot doc : transactionSnapshots) {
                                                                            batch.delete(doc.getReference());
                                                                        }

                                                                        // Delete all categories
                                                                        for (DocumentSnapshot doc : categorySnapshots) {
                                                                            batch.delete(doc.getReference());
                                                                        }

                                                                        // Delete all budgets
                                                                        for (DocumentSnapshot doc : budgetSnapshots) {
                                                                            batch.delete(doc.getReference());
                                                                        }

                                                                        // Delete all loans
                                                                        for (DocumentSnapshot doc : loanSnapshots) {
                                                                            batch.delete(doc.getReference());
                                                                        }
                                                                    }).addOnSuccessListener(aVoid -> {
                                                                        Log.d(TAG, "User data deleted successfully");
                                                                        if (listener != null) listener.onSuccess();
                                                                    }).addOnFailureListener(e -> {
                                                                        Log.e(TAG, "Error deleting user data", e);
                                                                        if (listener != null) listener.onFailure(e);
                                                                    });
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e(TAG, "Error fetching loans", e);
                                                                    if (listener != null) listener.onFailure(e);
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error fetching budgets", e);
                                                        if (listener != null) listener.onFailure(e);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error fetching categories", e);
                                            if (listener != null) listener.onFailure(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching transactions", e);
                                if (listener != null) listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching accounts", e);
                    if (listener != null) listener.onFailure(e);
                });
    }


    /**
     * Updates a specific field in the user profile document
     *
     * @param profile The UserProfile object containing the updated field
     * @param fieldName The name of the field to update (e.g., "displayName", "photoUrl")
     * @param listener Callback to handle the result of the operation
     */
    public void updateUserProfileField(UserProfile profile, String fieldName, OnProfileUpdateListener listener) {
        if (profile == null || profile.getUid() == null || fieldName == null || fieldName.isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("Profile, user ID, or field name cannot be null"));
            return;
        }

        String userId = profile.getUid();
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(userId);

        // Get the value for the specified field from the profile object
        Object fieldValue = null;
        switch (fieldName) {
            case "displayName":
                fieldValue = profile.getDisplayName();
                break;
            case "photoUrl":
                fieldValue = profile.getPhotoUrl();
                break;
            case "phoneNumber":
                fieldValue = profile.getPhoneNumber();
                break;
            case "location":
                fieldValue = profile.getLocation();
                break;
            case "darkModeEnabled":
                fieldValue = profile.isDarkModeEnabled();
                break;
            case "notificationsEnabled":
                fieldValue = profile.isNotificationsEnabled();
                break;
            case "defaultCurrency":
                fieldValue = profile.getDefaultCurrency();
                break;
            default:
                if (listener != null) listener.onFailure(new IllegalArgumentException("Unknown field name: " + fieldName));
                return;
        }

        // Update the field in Firestore
        userDocRef.update(fieldName, fieldValue)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile field " + fieldName + " updated successfully");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile field " + fieldName, e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    /**
     * Gets the user profile from Firestore
     *
     * @param uid The user ID
     * @param listener Callback to handle the result of the operation
     */
    public void getUserProfile(String uid, OnProfileLoadListener listener) {
        if (uid == null || uid.isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(uid);
        userDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                        if (userProfile != null) {
                            // Make sure UID is set
                            userProfile.setUid(uid);
                            if (listener != null) listener.onSuccess(userProfile);
                        } else {
                            if (listener != null) listener.onFailure(new Exception("Failed to parse user profile"));
                        }
                    } else {
                        // Document doesn't exist, create a new default profile
                        UserProfile newProfile = new UserProfile(uid, "", "");
                        if (listener != null) listener.onSuccess(newProfile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    public interface OnProfileLoadListener {
        void onSuccess(UserProfile profile);
        void onFailure(Exception e);
    }

    public interface OnProfileUpdateListener {
        void onSuccess();
        void onFailure(Exception e);
    }
    public interface OnProfileLoadedListener {
        void onSuccess(Profile profile);
        void onFailure(Exception e);
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }
    public interface OnSharedAccountAddedListener { void onSuccess(String id); void onFailure(Exception e); }
    public interface OnSharedAccountRetrievedListener { void onSuccess(Account account); void onFailure(Exception e); }
    public interface OnSharedAccountIdsRetrievedListener { void onSuccess(List<String> ids); void onFailure(Exception e); } // Có thể không cần nữa
    public interface OnShareUpdatedListener { void onSuccess(); void onFailure(Exception e); } // Dùng chung OnAccountUpdatedListener?
    public interface OnMembershipUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnSharedAccountRefsRetrievedListener { void onSuccess(List<SharedAccountRef> refs); void onFailure(Exception e); }
    public interface OnDataInitializedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnTransactionAddedListener { void onSuccess(String transactionId); void onFailure(Exception e); }
    // *** SỬA LẠI INTERFACE NÀY ***
    public interface OnTransactionsRetrievedListener { void onSuccess(List<Transaction> transactions); void onFailure(Exception e); } // Plural
    public interface OnTransactionRetrievedListener { void onSuccess(Transaction transaction);

        void onFailure(Exception e); } // Singular
    public interface OnTransactionUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnTransactionDeletedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnCategoryAddedListener { void onSuccess(String categoryId); void onFailure(Exception e); }
    public interface OnCategoriesRetrievedListener { void onSuccess(List<Category> categories); void onFailure(Exception e); }
    public interface OnCategoryRetrievedListener { void onSuccess(Category category); void onFailure(Exception e); }
    public interface OnCategoryUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnCategoryDeletedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnAccountAddedListener { void onSuccess(String accountId); void onFailure(Exception e); }
    public interface OnAccountsRetrievedListener { void onSuccess(List<Account> accounts); void onFailure(Exception e); } // Plural
    public interface OnAccountRetrievedListener { void onSuccess(Account account); void onFailure(Exception e); }
    public interface OnAccountUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnAccountDeletedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnLoanRetrievedListener { void onSuccess(Loan loan); void onFailure(Exception e); }

    public interface OnBudgetAddedListener { void onSuccess(String id); void onFailure(Exception e); }
    public interface OnBudgetsRetrievedListener { void onSuccess(List<Budget> list); void onFailure(Exception e); }
    public interface OnBudgetUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnBudgetDeletedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnLoanAddedListener { void onSuccess(String id); void onFailure(Exception e); }
    public interface OnLoansRetrievedListener { void onSuccess(List<Loan> list); void onFailure(Exception e); }
    public interface OnLoanUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnLoanDeletedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnRepaymentAddedListener { void onSuccess(); void onFailure(Exception e); } // Maybe return ID?
    public interface OnRepaymentsRetrievedListener { void onSuccess(List<Repayment> list); void onFailure(Exception e); }
    public interface OnReminderAddedListener { void onSuccess(String id); void onFailure(Exception e); }
    public interface OnRemindersRetrievedListener { void onSuccess(List<PaymentReminder> list); void onFailure(Exception e); }
    public interface OnReminderUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnReminderDeletedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnUserFoundListener { void onSuccess(String userId); void onFailure(Exception e); }
}