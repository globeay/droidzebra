/* Copyright (C) 2010 by Alex Kompel  */
/* This file is part of DroidZebra.

    DroidZebra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DroidZebra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DroidZebra.  If not, see <http://www.gnu.org/licenses/>
*/

package com.shurik.droidzebra;

import java.util.Calendar;
import java.util.Date;

import com.shurik.droidzebra.ZebraEngine;
import com.shurik.droidzebra.ZebraEngine.CandidateMove;
import com.shurik.droidzebra.ZebraEngine.Move;
import com.shurik.droidzebra.ZebraEngine.PlayerInfo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class DroidZebra extends FragmentActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	public static final String SHARED_PREFS_NAME="droidzebrasettings";

	final public static int boardSize = 8;

	private static final int
	MENU_NEW_GAME = 1,
	MENU_QUIT = 2,
	MENU_TAKE_BACK = 3,
	MENU_SETTINGS = 4,
	MENU_SWITCH_SIDES = 5,
	MENU_DONATE = 6,
	MENU_MAIL = 7,
	MENU_TAKE_REDO = 8,
	MENU_HINT = 9
	;

	private static final int
	FUNCTION_HUMAN_VS_HUMAN = 0,
	FUNCTION_ZEBRA_WHITE = 1,
	FUNCTION_ZEBRA_BLACK = 2,
	FUNCTION_ZEBRA_VS_ZEBRA = 3;

	private static final int
	RANDOMNESS_NONE = 0,
	RANDOMNESS_SMALL = 1,
	RANDOMNESS_MEDIUM = 2,
	RANDOMNESS_LARGE = 3,
	RANDOMNESS_HUGE = 4;
	
	public ZebraEngine mZebraThread;

	public static final int DEFAULT_SETTING_FUNCTION = FUNCTION_ZEBRA_WHITE;
	public static final String DEFAULT_SETTING_STRENGTH  = "1|1|1";
	public static final boolean DEFAULT_SETTING_AUTO_MAKE_FORCED_MOVES  = false;
	public static final int DEFAULT_SETTING_RANDOMNESS = RANDOMNESS_LARGE;
	public static final String DEFAULT_SETTING_FORCE_OPENING = "None";
	public static final boolean DEFAULT_SETTING_HUMAN_OPENINGS = false;
	public static final boolean DEFAULT_SETTING_PRACTICE_MODE = false;
	public static final boolean DEFAULT_SETTING_USE_BOOK = true;
	public static final boolean DEFAULT_SETTING_DISPLAY_PV = true;
	public static final boolean DEFAULT_SETTING_DISPLAY_MOVES = true;
	public static final boolean DEFAULT_SETTING_DISPLAY_LAST_MOVE = true;
	public static final String DEFAULT_SETTING_SENDMAIL = "";
	public static final boolean DEFAULT_SETTING_DISPLAY_ENABLE_ANIMATIONS = true;

	public static final String 
	SETTINGS_KEY_FUNCTION = "settings_engine_function",
	SETTINGS_KEY_STRENGTH = "settings_engine_strength",
	SETTINGS_KEY_AUTO_MAKE_FORCED_MOVES = "settings_engine_auto_make_moves",
	SETTINGS_KEY_RANDOMNESS = "settings_engine_randomness",
	SETTINGS_KEY_FORCE_OPENING = "settings_engine_force_opening",
	SETTINGS_KEY_HUMAN_OPENINGS = "settings_engine_human_openings",
	SETTINGS_KEY_PRACTICE_MODE = "settings_engine_practice_mode",
	SETTINGS_KEY_USE_BOOK = "settings_engine_use_book",
	SETTINGS_KEY_DISPLAY_PV = "settings_ui_display_pv",
	SETTINGS_KEY_DISPLAY_MOVES = "settings_ui_display_moves",
	SETTINGS_KEY_DISPLAY_LAST_MOVE = "settings_ui_display_last_move",
	SETTINGS_KEY_SENDMAIL = "settings_sendmail",
	SETTINGS_KEY_DISPLAY_ENABLE_ANIMATIONS = "settings_ui_display_enable_animations"
	;

	public static class BoardState {
		public final static byte ST_FLIPPED = 0x01;
		public byte mState;
		public byte mFlags;
		public BoardState(byte state) {
			mState = state;
			mFlags = 0;
		}
		public void set(byte newState) {
			if(newState!=ZebraEngine.PLAYER_EMPTY && mState!=ZebraEngine.PLAYER_EMPTY && mState!=newState)
				mFlags |= ST_FLIPPED;
			else
				mFlags &= ~ST_FLIPPED;
			mState = newState;
		}
		public byte getState() {
			return mState;
		}
		public boolean isFlipped() {
			return (mFlags & ST_FLIPPED)>0;
		}
	};
	private BoardState mBoard[][] = new BoardState[boardSize][boardSize];
	
	private CandidateMove[] mCandidateMoves = null;

	private Move mLastMove = null;
	private int mWhiteScore = 0;
	private int mBlackScore = 0;
	private String mOpeningName;

	private BoardView mBoardView;
	private StatusView mStatusView;
	
	private boolean mBusyDialogUp = false;
	private boolean mHintIsUp = false;
	private boolean mIsInitCompleted = false;
	private boolean mActivityActive = false;
	
	private SharedPreferences mSettings;
	public int mSettingFunction = DEFAULT_SETTING_FUNCTION;
	private int mSettingZebraDepth = 1;
	private int mSettingZebraDepthExact = 1;
	private int mSettingZebraDepthWLD = 1;
	public boolean mSettingAutoMakeForcedMoves = DEFAULT_SETTING_AUTO_MAKE_FORCED_MOVES;
	public int mSettingZebraRandomness = DEFAULT_SETTING_RANDOMNESS;
	public String mSettingZebraForceOpening = DEFAULT_SETTING_FORCE_OPENING;
	public boolean mSettingZebraHumanOpenings = DEFAULT_SETTING_HUMAN_OPENINGS;
	public boolean mSettingZebraPracticeMode = DEFAULT_SETTING_PRACTICE_MODE;
	public boolean mSettingZebraUseBook = DEFAULT_SETTING_USE_BOOK;
	public boolean mSettingDisplayPV = DEFAULT_SETTING_DISPLAY_PV;
	public boolean mSettingDisplayMoves = DEFAULT_SETTING_DISPLAY_MOVES;
	public boolean mSettingDisplayLastMove = DEFAULT_SETTING_DISPLAY_LAST_MOVE;
	public boolean mSettingDisplayEnableAnimations = DEFAULT_SETTING_DISPLAY_ENABLE_ANIMATIONS;
	public int mSettingAnimationDelay = 1000;

	private void newCompletionPort(final int zebraEngineStatus, final Runnable completion) {
		new AsyncTask<Void, Void, Void>() {
			@Override 
			protected Void doInBackground(Void... p) {
				mZebraThread.waitForEngineState(zebraEngineStatus);
				return null;
			}
			@Override
			protected void onPostExecute(Void result) {
				completion.run();
			}
		}
		.execute();
	}

	public DroidZebra() {
		initBoard();
	}

	public BoardState[][] getBoard() {
		return mBoard;
	}

	public void initBoard() {
		mCandidateMoves = null;
		mLastMove = null;
		mWhiteScore = mBlackScore = 0;
		for(int i=0; i<boardSize; i++)
			for(int j=0; j<boardSize; j++)
				mBoard[i][j] = new BoardState(ZebraEngine.PLAYER_EMPTY);
		if( mStatusView!=null ) 
			mStatusView.clear();
	}

	public void setBoard(byte[] board) {
		for(int i=0; i<boardSize; i++)
			for(int j=0; j<boardSize; j++) {
				mBoard[i][j].set(board[i*boardSize + j]);
			}
	}

	public CandidateMove[] getCandidateMoves() {
		return mCandidateMoves;
	}

	public void setCandidateMoves(CandidateMove[] cmoves) {
		mCandidateMoves = cmoves;
		mBoardView.invalidate();
	}

	public void setLastMove(Move move) {
		mLastMove = move;
	}

	public Move getLastMove() {
		return mLastMove;
	}

	public boolean isValidMove(Move move) {
		if( mCandidateMoves==null ) 
			return false;
		for(CandidateMove m:mCandidateMoves) {
			if(m.mMove.getX()==move.getX() && m.mMove.getY()==move.getY() ) return true;
		}
		return false;
	}

	public boolean evalsDisplayEnabled() {
		return mSettingZebraPracticeMode || mHintIsUp;
	}
	
	public void newGame() {
		if(mZebraThread.getEngineState()!=ZebraEngine.ES_READY2PLAY) {
			mZebraThread.stopGame();
		}
		newCompletionPort(
				ZebraEngine.ES_READY2PLAY,
				new Runnable() {
					@Override public void run() {
						DroidZebra.this.initBoard();
						DroidZebra.this.loadSettings();
						DroidZebra.this.mZebraThread.setEngineState(ZebraEngine.ES_PLAY);                
					}
				}
		);
	}

	class DroidZebraHandler extends Handler {
		@Override
		public void handleMessage(Message m) {
			// block messages if waiting for something
			switch(m.what) {
			case ZebraEngine.MSG_ERROR: {
				FatalError(m.getData().getString("error"));
			} break;
			case ZebraEngine.MSG_MOVE_START: {
				// noop
			} break;
			case ZebraEngine.MSG_BOARD: {
				String score;
				int sideToMove = m.getData().getInt("side_to_move");
				
				setBoard(m.getData().getByteArray("board"));
				
				mBlackScore = m.getData().getBundle("black").getInt("disc_count");
				mWhiteScore = m.getData().getBundle("white").getInt("disc_count");
				
				if(sideToMove==ZebraEngine.PLAYER_BLACK) {
					score = String.format("•%d", mBlackScore);
				} else {
					score = String.format("%d", mBlackScore);
				}
				mStatusView.setTextForID(
					StatusView.ID_SCORE_BLACK, 
					score
				);

				if(sideToMove==ZebraEngine.PLAYER_WHITE) {
					score = String.format("%d•", mWhiteScore);
				} else {
					score = String.format("%d", mWhiteScore);
				}
				mStatusView.setTextForID(
						StatusView.ID_SCORE_WHITE,
						score
					);
				
				int iStart, iEnd;
				byte[] black_moves = m.getData().getBundle("black").getByteArray("moves");
				byte[] white_moves = m.getData().getBundle("white").getByteArray("moves");
				
				iEnd = black_moves.length;
				iStart = Math.max(0, iEnd-4);
				for( int i=0; i<4; i++ ) {
					String num_text = String.format("%d", i+iStart+1);
					String move_text;
					if( i+iStart<iEnd ) {
						Move move = new Move(black_moves[i+iStart]);
						move_text = move.getText();
					} else {
						move_text = "";
					}
					mStatusView.setTextForID(
							StatusView.ID_SCORELINE_NUM_1 + i,
							num_text
						);
					mStatusView.setTextForID(
							StatusView.ID_SCORELINE_BLACK_1 + i,
							move_text
						);
				}

				iEnd = white_moves.length;
				iStart = Math.max(0, iEnd-4);
				for( int i=0; i<4; i++ ) {
					String move_text;
					if( i+iStart<iEnd ) {
						Move move = new Move(white_moves[i+iStart]);
						move_text = move.getText();
					} else {
						move_text = "";
					}
					mStatusView.setTextForID(
							StatusView.ID_SCORELINE_WHITE_1 + i,
							move_text
						);
				}
				mBoardView.onBoardStateChanged();
			} break;
			
			case ZebraEngine.MSG_CANDIDATE_MOVES: {
				setCandidateMoves((CandidateMove[]) m.obj);
			} break;
			
			case ZebraEngine.MSG_PASS: {
				showPassDialog();
			} break;
			
			case ZebraEngine.MSG_OPENING_NAME: {
				mOpeningName = m.getData().getString("opening");
				mStatusView.setTextForID(
						StatusView.ID_STATUS_OPENING, 
						mOpeningName
					);
			} break;
			
			case ZebraEngine.MSG_LAST_MOVE: {
				byte move =  (byte)m.getData().getInt("move");
				setLastMove(move==Move.PASS ? null : new Move(move));
			} break;
			
			case ZebraEngine.MSG_GAME_OVER: {
				showGameOverDialog();
			} break;
			
			case ZebraEngine.MSG_EVAL_TEXT: {
				if( mSettingDisplayPV ) {
					mStatusView.setTextForID(
							StatusView.ID_STATUS_EVAL, 
							m.getData().getString("eval")
						);
				}
			} break;
			
			case ZebraEngine.MSG_PV: {
				if( mSettingDisplayPV ) {
					byte[] pv = m.getData().getByteArray("pv");
					String pvText = new String();
					for( byte move:pv ) {
						pvText += new Move(move).getText();
						pvText += " ";
					}
					mStatusView.setTextForID(
							StatusView.ID_STATUS_PV, 
							pvText
						);
				}
			} break;
			
			case ZebraEngine.MSG_MOVE_END: {
				dismissBusyDialog();
				if( mHintIsUp ) {
					mHintIsUp = false;
					mZebraThread.setPracticeMode(mSettingZebraPracticeMode);
					mZebraThread.sendSettingsChanged();
				}
			} break;
			
			case ZebraEngine.MSG_CANDIDATE_EVALS: {
				CandidateMove[] evals = (CandidateMove[]) m.obj;
				for(CandidateMove eval:evals) {
					for(int i=0; i<mCandidateMoves.length; i++) {
						if(mCandidateMoves[i].mMove.mMove == eval.mMove.mMove) {
							mCandidateMoves[i] = eval;
							break;
						}
					}
				}
				mBoardView.invalidate();
			} break;
			
			case ZebraEngine.MSG_DEBUG: {
//				Log.d("DroidZebra", m.getData().getString("message"));
			} break;
			}
		}
	}
	private DroidZebraHandler mDroidZebraHandler = null;
	
	/* Creates the menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_NEW_GAME, 0, R.string.menu_item_new_game).setIcon(R.drawable.ic_menu_play);
		menu.add(0, MENU_HINT, 0, R.string.menu_item_hint).setIcon(android.R.drawable.ic_menu_compass);
		menu.add(0, MENU_TAKE_BACK, 0, R.string.menu_item_undo).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0, MENU_TAKE_REDO, 0, R.string.menu_item_redo).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_SETTINGS, 0, R.string.menu_item_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_SWITCH_SIDES, 0, R.string.menu_item_switch_sides).setIcon(R.drawable.ic_menu_switch_sides);
		menu.add(0, MENU_DONATE, 0, R.string.menu_item_donate).setIcon(android.R.drawable.ic_menu_send);		
		menu.add(0, MENU_MAIL, 0, R.string.menu_item_mail).setIcon(android.R.drawable.ic_menu_send);
		menu.add(0, MENU_QUIT, 0, R.string.menu_item_quit).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if( !mIsInitCompleted ) return false;
		try {
			switch (item.getItemId()) {
			case MENU_NEW_GAME:
				newGame();
				return true;
			case MENU_QUIT:
				showQuitDialog();
				return true;
			case MENU_TAKE_BACK:
				mZebraThread.undoMove();
				return true;
			case MENU_TAKE_REDO:
				mZebraThread.redoMove();
				return true;
			case MENU_SETTINGS: {
				// Launch Preference activity
				Intent i = new Intent(this, SettingsPreferences.class);
				startActivity(i);
			} return true;
			case MENU_SWITCH_SIDES: {
				switchSides();
			} break;
			case MENU_DONATE: {
				showDonateDialog();
			} return true;
			case MENU_MAIL: {
				sendMail();
			} return true;
			case MENU_HINT: {
				showHint();
			} return true;
			}
		} catch (EngineError e) {
			FatalError(e.msg);
		}
		return false;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.spash_layout);

		// start your engines
		mDroidZebraHandler = new DroidZebraHandler();
		mZebraThread = new ZebraEngine(this, mDroidZebraHandler);

		// preferences
		mSettings = getSharedPreferences(SHARED_PREFS_NAME, 0);
		mSettings.registerOnSharedPreferenceChangeListener(this);

		if( savedInstanceState != null 
			&& savedInstanceState.containsKey("moves_played_count") ) {
			mZebraThread.setInitialGameState(savedInstanceState.getInt("moves_played_count"), savedInstanceState.getByteArray("moves_played"));
		} 
		/*else {
			//byte[] init = {56,66,65,46,35,64,47,34,33,36,57,24,43,25,37,23,63,26,16,15,14,13,12,53, 52, 62, 75, 41, 42, 74, 51, 31, 32, 61, 83, 84, 73, 82, 17, 21, 72, 68, 58, 85, 76, 67, 86, 87, 78, 38, 48, 88, 27, 77 };
			byte[] init = {
					65 , 46 , 35 , 64 , 53 , 36 , 56 , 24 , 27 , 34 , 26 , 43 , 33 , 25 , 47 , 18 ,
					15 , 14 , 37 , 16 , 17 , 62 , 23 , 52 , 13 , 66 , 74 , 12 , 63 , 42 , 32 , 41 ,
					31 , 51 , 22 , 21 , 72 , 11 , 67 , 28 , 38 , 58 , 48 , 61 , 68 , 57 , -1 , 71 ,
					-1 , 81 , 82 , 78 , -1 , 77 , -1 , 83 , 84 , 73 , 75 , 86 , 76 , 87 
			};
			mZebraThread.setInitialGameState(init.length, init);
		}*/
		
		mZebraThread.start();

		newCompletionPort(
				ZebraEngine.ES_READY2PLAY,
				new Runnable() {
					@Override public void run() {
						DroidZebra.this.setContentView(R.layout.board_layout);
						DroidZebra.this.mBoardView = (BoardView)DroidZebra.this.findViewById(R.id.board);
						DroidZebra.this.mStatusView  = (StatusView)DroidZebra.this.findViewById(R.id.status_panel);
						DroidZebra.this.mBoardView.setDroidZebra(DroidZebra.this);
						DroidZebra.this.mBoardView.requestFocus();
						DroidZebra.this.initBoard();
						DroidZebra.this.loadSettings();
						DroidZebra.this.mZebraThread.setEngineState(ZebraEngine.ES_PLAY);                
						DroidZebra.this.mIsInitCompleted = true;
					}
				}
		);
	}

	private void loadSettings() {
		int settingsFunction, settingZebraDepth, settingZebraDepthExact, settingZebraDepthWLD;
		int settingRandomness;
		boolean settingAutoMakeForcedMoves;
		String settingZebraForceOpening;
		boolean settingZebraHumanOpenings;
		boolean settingZebraPracticeMode;
		boolean settingZebraUseBook;
		
		SharedPreferences settings = getSharedPreferences(SHARED_PREFS_NAME, 0);

		settingsFunction = Integer.parseInt(settings.getString(SETTINGS_KEY_FUNCTION, String.format("%d", DEFAULT_SETTING_FUNCTION)));
		String[] strength = settings.getString(SETTINGS_KEY_STRENGTH, DEFAULT_SETTING_STRENGTH).split("\\|");
		// Log.d("DroidZebra", String.format("settings %s:%s|%s|%s", SETTINGS_KEY_STRENGTH, strength[0], strength[1], strength[2]));

		settingZebraDepth = Integer.parseInt(strength[0]);
		settingZebraDepthExact = Integer.parseInt(strength[1]);
		settingZebraDepthWLD = Integer.parseInt(strength[2]);
		//Log.d( "DroidZebra", 
		//		String.format("Function: %d; depth: %d; exact: %d; wld %d",
		//				mSettingFunction, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD)
		//);

		settingAutoMakeForcedMoves = settings.getBoolean(SETTINGS_KEY_AUTO_MAKE_FORCED_MOVES, DEFAULT_SETTING_AUTO_MAKE_FORCED_MOVES);
		settingRandomness = Integer.parseInt(settings.getString(SETTINGS_KEY_RANDOMNESS, String.format("%d", DEFAULT_SETTING_RANDOMNESS)));
		settingZebraForceOpening = settings.getString(SETTINGS_KEY_FORCE_OPENING, DEFAULT_SETTING_FORCE_OPENING);
		settingZebraHumanOpenings = settings.getBoolean(SETTINGS_KEY_HUMAN_OPENINGS, DEFAULT_SETTING_HUMAN_OPENINGS);
		settingZebraPracticeMode = settings.getBoolean(SETTINGS_KEY_PRACTICE_MODE, DEFAULT_SETTING_PRACTICE_MODE);
		settingZebraUseBook = settings.getBoolean(SETTINGS_KEY_USE_BOOK, DEFAULT_SETTING_USE_BOOK);

		
		boolean bZebraSettingChanged = (
			mSettingFunction != settingsFunction 
			|| mSettingZebraDepth != settingZebraDepth
			|| mSettingZebraDepthExact != settingZebraDepthExact
			|| mSettingZebraDepthWLD != settingZebraDepthWLD
			|| mSettingAutoMakeForcedMoves != settingAutoMakeForcedMoves
			|| mSettingZebraRandomness != settingRandomness
			|| mSettingZebraForceOpening != settingZebraForceOpening
			|| mSettingZebraHumanOpenings != settingZebraHumanOpenings
			|| mSettingZebraPracticeMode != settingZebraPracticeMode
			|| mSettingZebraUseBook != settingZebraUseBook
			);
		
		mSettingFunction = settingsFunction;
		mSettingZebraDepth = settingZebraDepth;
		mSettingZebraDepthExact = settingZebraDepthExact;
		mSettingZebraDepthWLD = settingZebraDepthWLD;
		mSettingAutoMakeForcedMoves = settingAutoMakeForcedMoves;
		mSettingZebraRandomness = settingRandomness;
		mSettingZebraForceOpening = settingZebraForceOpening;
		mSettingZebraHumanOpenings = settingZebraHumanOpenings;
		mSettingZebraPracticeMode = settingZebraPracticeMode;
		mSettingZebraUseBook = settingZebraUseBook;
		
		try {
			mZebraThread.setAutoMakeMoves(mSettingAutoMakeForcedMoves);
			mZebraThread.setForcedOpening(mSettingZebraForceOpening);
			mZebraThread.setHumanOpenings(mSettingZebraHumanOpenings);
			mZebraThread.setPracticeMode(mSettingZebraPracticeMode);
			mZebraThread.setUseBook(mSettingZebraUseBook);
			
			switch( mSettingFunction ) {
			case FUNCTION_HUMAN_VS_HUMAN:
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_BLACK, 0, 0, 0, ZebraEngine.INFINIT_TIME, 0));
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_WHITE, 0, 0, 0, ZebraEngine.INFINIT_TIME, 0));
				break;
			case FUNCTION_ZEBRA_BLACK:
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_BLACK, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ZebraEngine.INFINIT_TIME, 0));
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_WHITE, 0, 0, 0, ZebraEngine.INFINIT_TIME, 0));
				break;
			case FUNCTION_ZEBRA_VS_ZEBRA:
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_BLACK, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ZebraEngine.INFINIT_TIME, 0));
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_WHITE, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ZebraEngine.INFINIT_TIME, 0));
				break;
			case FUNCTION_ZEBRA_WHITE:
			default:
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_BLACK, 0, 0, 0, ZebraEngine.INFINIT_TIME, 0));
				mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_WHITE, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ZebraEngine.INFINIT_TIME, 0));
				break;
			}
			mZebraThread.setPlayerInfo(new PlayerInfo(ZebraEngine.PLAYER_ZEBRA, mSettingZebraDepth+1, mSettingZebraDepthExact+1, mSettingZebraDepthWLD+1, ZebraEngine.INFINIT_TIME, 0));
			
			switch(mSettingZebraRandomness) {
			case RANDOMNESS_SMALL:
				mZebraThread.setSlack(1.5f);
				mZebraThread.setPerturbation(1.0f);
				break;
			case RANDOMNESS_MEDIUM:
				mZebraThread.setSlack(4.0f);
				mZebraThread.setPerturbation(2.5f);
				break;
			case RANDOMNESS_LARGE:
				mZebraThread.setSlack(6.0f);
				mZebraThread.setPerturbation(6.0f);
				break;
			case RANDOMNESS_HUGE:
				mZebraThread.setSlack(10.0f);
				mZebraThread.setPerturbation(16.0f);
				break;
			case RANDOMNESS_NONE:
			default:
				mZebraThread.setSlack(0.0f);
				mZebraThread.setPerturbation(0.0f);
				break;
			}
		} catch (EngineError e) {
			FatalError(e.msg);
		}
		
		mStatusView.setTextForID(
			StatusView.ID_SCORE_SKILL, 
			String.format(getString(R.string.display_depth), mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD)
			);

		mSettingDisplayPV = settings.getBoolean(SETTINGS_KEY_DISPLAY_PV, DEFAULT_SETTING_DISPLAY_PV);
		if(!mSettingDisplayPV ) {
			mStatusView.setTextForID(StatusView.ID_STATUS_PV, "");
			mStatusView.setTextForID(StatusView.ID_STATUS_EVAL, "");
		}

		mSettingDisplayMoves = settings.getBoolean(SETTINGS_KEY_DISPLAY_MOVES, DEFAULT_SETTING_DISPLAY_MOVES);
		mSettingDisplayLastMove = settings.getBoolean(SETTINGS_KEY_DISPLAY_LAST_MOVE, DEFAULT_SETTING_DISPLAY_LAST_MOVE);
		
		mSettingDisplayEnableAnimations = settings.getBoolean(SETTINGS_KEY_DISPLAY_ENABLE_ANIMATIONS, DEFAULT_SETTING_DISPLAY_ENABLE_ANIMATIONS);
		mZebraThread.setMoveDelay(mSettingDisplayEnableAnimations? mSettingAnimationDelay + 1000 : 0);
		
		if( bZebraSettingChanged ) {
			mZebraThread.sendSettingsChanged();
		}
	}

	private void sendMail(){
		//GetNowTime
		Calendar calendar = Calendar.getInstance();
		Date nowTime = calendar.getTime(); 
		StringBuffer sbBlackPlayer = new StringBuffer();
		StringBuffer sbWhitePlayer = new StringBuffer();
		ZebraEngine.GameState gs = mZebraThread.getGameState(); 
		SharedPreferences settings = getSharedPreferences(SHARED_PREFS_NAME, 0);
		byte[] moves = null;
		if( gs != null ) {
			moves = gs.mMoveSequence;
		}
	
		Intent intent = new Intent();  
		intent.setAction(Intent.ACTION_SEND);  
		intent.setType("message/rfc822");  
		intent.putExtra(
				Intent.EXTRA_EMAIL,
				new String[]{settings.getString(SETTINGS_KEY_SENDMAIL, DEFAULT_SETTING_SENDMAIL)});  
		
		intent.putExtra(Intent.EXTRA_SUBJECT, "DroidZebra");  
	
		//get BlackPlayer and WhitePlayer
		switch( mSettingFunction ) {
			case FUNCTION_HUMAN_VS_HUMAN:
				sbBlackPlayer.append("Player");
				sbWhitePlayer.append("Player");
				break;
			case FUNCTION_ZEBRA_BLACK:
				sbBlackPlayer.append("DroidZebra-");
				sbBlackPlayer.append(mSettingZebraDepth);
				sbBlackPlayer.append("/");
				sbBlackPlayer.append(mSettingZebraDepthExact);
				sbBlackPlayer.append("/");
				sbBlackPlayer.append(mSettingZebraDepthWLD );
				
				sbWhitePlayer.append("Player");
				break;
			case FUNCTION_ZEBRA_WHITE:
				sbBlackPlayer.append("Player");
				
				sbWhitePlayer.append("DroidZebra-");
				sbWhitePlayer.append(mSettingZebraDepth);
				sbWhitePlayer.append("/");
				sbWhitePlayer.append(mSettingZebraDepthExact);
				sbWhitePlayer.append("/");
				sbWhitePlayer.append(mSettingZebraDepthWLD );
				break;
			case FUNCTION_ZEBRA_VS_ZEBRA:
				sbBlackPlayer.append("DroidZebra-");
				sbBlackPlayer.append(mSettingZebraDepth);
				sbBlackPlayer.append("/");
				sbBlackPlayer.append(mSettingZebraDepthExact);
				sbBlackPlayer.append("/");
				sbBlackPlayer.append(mSettingZebraDepthWLD );
				
				sbWhitePlayer.append("DroidZebra-");
				sbWhitePlayer.append(mSettingZebraDepth);
				sbWhitePlayer.append("/");
				sbWhitePlayer.append(mSettingZebraDepthExact);
				sbWhitePlayer.append("/");
				sbWhitePlayer.append(mSettingZebraDepthWLD );
			default:
		}
		StringBuffer sb = new StringBuffer();
		sb.append(getResources().getString(R.string.mail_generated));
		sb.append("\r\n");
		sb.append(getResources().getString(R.string.mail_date));
		sb.append(" ");
		sb.append(nowTime);
		sb.append("\r\n\r\n");
		sb.append(getResources().getString(R.string.mail_move));
		sb.append(" ");
		StringBuffer sbMoves = new StringBuffer();
		if(moves != null){
			
			for(int i=0; i < moves.length; i++){
				if(moves[i]!=0x00){
					Move move = new Move(moves[i]);
					sbMoves.append(move.getText());
					if(mLastMove.getText().equals(move.getText())){
						break;
					}
				}
			}
		}
		sb.append(sbMoves);
		sb.append("\r\n\r\n");
		sb.append(sbBlackPlayer.toString());
		sb.append("  (B)  ");
		sb.append(mBlackScore);
		sb.append(":");
		sb.append(mWhiteScore);
		sb.append("  (W)  ");
		sb.append(sbWhitePlayer.toString());
		sb.append("\r\n\r\n");
		sb.append(getResources().getString(R.string.mail_url));
		sb.append("\r\n");	
		sb.append(getResources().getString(R.string.mail_viewer_url));
		sb.append("lm=");
		sb.append(sbMoves);
		sb.append("&bp=");
		sb.append(sbBlackPlayer.toString());
		sb.append("&wp=");
		sb.append(sbWhitePlayer.toString());
		sb.append("&bs=");
		sb.append(mBlackScore);
		sb.append("&ws=");
		sb.append(mWhiteScore);
		
		intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
		// Intent 
		this.startActivity(intent);  
	}
	
	private void switchSides() {
		int newFunction = -1;
		
		if( mSettingFunction == FUNCTION_ZEBRA_WHITE )
			newFunction = FUNCTION_ZEBRA_BLACK;
		else if( mSettingFunction == FUNCTION_ZEBRA_BLACK )
			newFunction = FUNCTION_ZEBRA_WHITE;
		
		if(newFunction>0) {
			SharedPreferences settings = getSharedPreferences(SHARED_PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(SETTINGS_KEY_FUNCTION, String.format("%d", newFunction));
			editor.commit();
		}
		
		loadSettings();
		
		// start a new game if not playing
		if( !mZebraThread.gameInProgress() )
			newGame();
	}
	
	private void showHint() {
		if( !mSettingZebraPracticeMode ) {
			mHintIsUp = true;
			mZebraThread.setPracticeMode(true);
			mZebraThread.sendSettingsChanged();
		}
	}
	
	@Override
	protected void onDestroy() {
		boolean retry = true;
		mZebraThread.setRunning(false);
		mZebraThread.interrupt(); // if waiting
		while (retry) {
			try {
				mZebraThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
		mDroidZebraHandler = null;
		super.onDestroy();
	}

	void showDialog(DialogFragment dialog, String tag) {
		if( mActivityActive ) {
			dialog.show(getSupportFragmentManager(), tag);
		}
	}
	
	//-------------------------------------------------------------------------
	// Donate Dialog
	public static class DialogDonate extends DialogFragment {

		public static DialogDonate newInstance() {
	    	return new DialogDonate();
	    }

	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_donate_title)
			.setMessage(R.string.dialog_donate_message)
			.setIcon(R.drawable.icon)
			.setPositiveButton( R.string.dialog_donate_doit, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String url = getResources().getString(R.string.donate_url);
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				}
			}
			)
			.setNeutralButton(R.string.dialog_donate_share, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String shareText = getResources().getString(R.string.donate_share_text);
					String shareTitle = getResources().getString(R.string.dialog_donate_share);
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_TEXT, shareText);
					startActivity(Intent.createChooser(intent, shareTitle));				}
			}
			)
			.setNegativeButton(R.string.dialog_donate_cancel, null)
			.create();
	    }
	}
	
	public void showDonateDialog() {
	    DialogFragment newFragment = DialogDonate.newInstance();
	    showDialog(newFragment, "dialog_donate");
	}

	//-------------------------------------------------------------------------
	// Pass Dialog
	public static class DialogPass extends DialogFragment {

		public static DialogPass newInstance() {
	    	return new DialogPass();
	    }

		public DroidZebra getDroidZebra() {
			return (DroidZebra)getActivity();
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.app_name)
			.setMessage(R.string.dialog_pass_text)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						getDroidZebra().mZebraThread.setEngineState(ZebraEngine.ES_PLAY);
					}
				}
			)
			.create();
	    }
	}
	
	public void showPassDialog() {
	    DialogFragment newFragment = DialogPass.newInstance();
	    showDialog(newFragment, "dialog_pass");
	}

	//-------------------------------------------------------------------------
	// Game Over Dialog
	public static class DialogGameOver extends DialogFragment {

		public static DialogGameOver newInstance() {
	    	return new DialogGameOver();
	    }

		public DroidZebra getDroidZebra() {
			return (DroidZebra)getActivity();
		}

		public void refreshContent(View dialog) {
			int winner;
			int blackScore = getDroidZebra().mBlackScore;
			int whiteScore = getDroidZebra().mWhiteScore;
			if( whiteScore>blackScore ) 
				winner =R.string.gameover_text_white_wins;
			else if( whiteScore<blackScore ) 
				winner = R.string.gameover_text_black_wins;
			else 
				winner = R.string.gameover_text_draw;

			((TextView)dialog.findViewById(R.id.gameover_text)).setText(winner);

			((TextView)dialog.findViewById(R.id.gameover_score)).setText(String.format("%d : %d", blackScore, whiteScore));
		}
		
	   @Override
	   public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
		   
		   	getDialog().setTitle(R.string.gameover_title);

			View v = inflater.inflate(R.layout.gameover, container, false);
			
			Button button;
			button = (Button)v.findViewById(R.id.gameover_choice_new_game);
			button.setOnClickListener(
					new View.OnClickListener() {
						public void onClick(View v) {
							dismiss();
							getDroidZebra().newGame();
						}
					});

			button = (Button)v.findViewById(R.id.gameover_choice_switch);
			button.setOnClickListener(
					new View.OnClickListener() {
						public void onClick(View v) {
							dismiss();
							getDroidZebra().switchSides();
						}
					});

			button = (Button)v.findViewById(R.id.gameover_choice_cancel);
			button.setOnClickListener(
					new View.OnClickListener() {
						public void onClick(View v) {
							dismiss();
						}
					});

			button = (Button)v.findViewById(R.id.gameover_choice_options);
			button.setOnClickListener(
					new View.OnClickListener() {
						public void onClick(View v) {
							dismiss();

							// start settings
							Intent i = new Intent(getDroidZebra(), SettingsPreferences.class);
							startActivity(i);
						}
					});
			
			button = (Button)v.findViewById(R.id.gameover_choice_email);
			button.setOnClickListener(
					new View.OnClickListener() {
						public void onClick(View v) {
							dismiss();
							getDroidZebra().sendMail();
						}
					});
			
			refreshContent(v);
			
			return v;
		}
	   
	   @Override
	   public void onResume() {
	     super.onResume();
	     refreshContent(getView());
	   }
	}
	
	public void showGameOverDialog() {
	    DialogFragment newFragment = DialogGameOver.newInstance();
	    showDialog(newFragment, "dialog_gameover");
	}

	//-------------------------------------------------------------------------
	// Pass Dialog
	public static class DialogQuit extends DialogFragment {

		public static DialogQuit newInstance() {
	    	return new DialogQuit();
	    }

		public DroidZebra getDroidZebra() {
			return (DroidZebra)getActivity();
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_quit_title)
			.setPositiveButton( R.string.dialog_quit_button_quit, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							getDroidZebra().finish();
						}
					}
				)
			.setNegativeButton( R.string.dialog_quit_button_cancel, null )
			.create();
	    }
	}
	
	public void showQuitDialog() {
	    DialogFragment newFragment = DialogQuit.newInstance();
	    showDialog(newFragment, "dialog_quit");
	}

	//-------------------------------------------------------------------------
	// Pass Dialog
	public static class DialogBusy extends DialogFragment {

		public static DialogBusy newInstance() {
	    	return new DialogBusy();
	    }

		public DroidZebra getDroidZebra() {
			return (DroidZebra)getActivity();
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
			ProgressDialog pd = new ProgressDialog(getActivity()) {
				@Override 
				public boolean onKeyDown(int keyCode, KeyEvent event) {
					 if( getDroidZebra().mZebraThread.isThinking() ) {
						 getDroidZebra().mZebraThread.stopMove();
					 }
					 getDroidZebra().mBusyDialogUp = false;
					 cancel();
					 return super.onKeyDown(keyCode, event);
				}
				@Override 
				public boolean onTouchEvent(MotionEvent event) {
					 if(event.getAction()==MotionEvent.ACTION_DOWN) {
						 if( getDroidZebra().mZebraThread.isThinking() ) {
							 getDroidZebra().mZebraThread.stopMove();
						 }
						 getDroidZebra().mBusyDialogUp = false;
						 cancel();
						 return true;
					 }
					return super.onTouchEvent(event);
				 }
			};
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setMessage(getResources().getString(R.string.dialog_busy_message));			
			return pd;
	    }
	}

	public void showBusyDialog() {
		if( !mBusyDialogUp && mZebraThread.isThinking() ) {
		    DialogFragment newFragment = DialogBusy.newInstance();
			mBusyDialogUp = true;
			showDialog(newFragment, "dialog_busy");
		}
	}

	public void dismissBusyDialog() {
		if( mBusyDialogUp ) {
		    Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog_busy");
		    if (prev != null) {
		        DialogFragment df = (DialogFragment) prev;
		        df.dismiss();
		    }		
			mBusyDialogUp = false;
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(mZebraThread!=null) 
			loadSettings();
	}

	public void FatalError(String msg) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("Zebra Error");
		alertDialog.setMessage(msg);
		alertDialog.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				DroidZebra.this.finish();
			}
		}
		);
		alertDialog.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mActivityActive = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mActivityActive = true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if( keyCode==KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0) {
			try {
				mZebraThread.undoMove();
			} catch (EngineError e) {
				FatalError(e.msg);
			}
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	/* requires api level 5 
	@Override
	public void onBackPressed() {
		try {
			mZebraThread.undoMove();
		} catch (EngineError e) {
			FatalError(e.msg);
		}
	} */

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		ZebraEngine.GameState gs = mZebraThread.getGameState(); 
		if( gs != null ) {
			outState.putByteArray("moves_played", gs.mMoveSequence);
			outState.putInt("moves_played_count", gs.mDisksPlayed);
			outState.putInt("version", 1);
		}
	}



}
